package com.tomo.tomoassistant.service.embedding;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.tomo.tomoassistant.config.LlmModelHolder;
import com.tomo.tomoassistant.config.properties.MilvusProperties;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;

import io.milvus.client.MilvusServiceClient;
import io.milvus.common.clientenum.ConsistencyLevelEnum;
import io.milvus.grpc.*;
import io.milvus.param.IndexType;
import io.milvus.param.MetricType;
import io.milvus.param.R;
import io.milvus.param.collection.*;
import io.milvus.param.dml.*;
import io.milvus.param.index.CreateIndexParam;
import io.milvus.response.SearchResultsWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmbeddingServiceImpl implements EmbeddingService {

    private final LlmModelHolder modelHolder;
    private final MilvusServiceClient milvusServiceClient;
    private final MilvusProperties milvusProperties;
    private final Gson gson = new Gson();

    @Override
    public List<float[]> embed(List<String> texts) {
        List<TextSegment> segments = texts.stream()
                .map(TextSegment::from)
                .toList();
        List<Embedding> embeddings = modelHolder.getEmbeddingModel().embedAll(segments).content();
        return embeddings.stream()
                .map(Embedding::vector)
                .toList();
    }

    @Override
    public float[] embedQuery(String text) {
        return modelHolder.getEmbeddingModel().embed(text).content().vector();
    }

    @Override
    public void storeEmbeddings(Long knowledgeBaseId, List<EmbeddedChunk> chunks) {
        if (chunks.isEmpty()) return;

        String collectionName = getCollectionName(knowledgeBaseId);
        ensureCollection(collectionName);

        List<JsonObject> rows = new ArrayList<>();
        for (EmbeddedChunk chunk : chunks) {
            JsonObject row = new JsonObject();
            row.addProperty("content", chunk.content());
            row.add("vector", gson.toJsonTree(chunk.vector()));
            row.addProperty("document_id", chunk.documentId());
            row.addProperty("chunk_index", chunk.chunkIndex());
            row.addProperty("metadata", gson.toJson(chunk.metadata()));
            rows.add(row);
        }

        InsertParam insertParam = InsertParam.newBuilder()
                .withCollectionName(collectionName)
                .withRows(rows)
                .build();
        milvusServiceClient.insert(insertParam);
        log.info("Stored {} embeddings in collection: {}", chunks.size(), collectionName);
    }

    @Override
    public void deleteEmbeddings(Long knowledgeBaseId, String documentId) {
        String collectionName = getCollectionName(knowledgeBaseId);
        String expr = "document_id == \"" + documentId + "\"";
        milvusServiceClient.delete(DeleteParam.newBuilder()
                .withCollectionName(collectionName)
                .withExpr(expr)
                .build());
        log.info("Deleted embeddings for document {} in collection: {}", documentId, collectionName);
    }

    @Override
    public List<SearchResult> search(Long knowledgeBaseId, float[] queryVector, int topK) {
        String collectionName = getCollectionName(knowledgeBaseId);

        if (!collectionExists(collectionName)) {
            return Collections.emptyList();
        }

        List<Float> vectorList = new ArrayList<>(queryVector.length);
        for (float v : queryVector) {
            vectorList.add(v);
        }

        SearchParam searchParam = SearchParam.newBuilder()
                .withCollectionName(collectionName)
                .withVectors(Collections.singletonList(vectorList))
                .withVectorFieldName("vector")
                .withTopK(topK)
                .withMetricType(MetricType.COSINE)
                .withOutFields(List.of("content", "document_id", "chunk_index", "metadata"))
                .withConsistencyLevel(ConsistencyLevelEnum.BOUNDED)
                .build();

        R<SearchResults> response = milvusServiceClient.search(searchParam);
        if (response.getStatus() != R.Status.Success.getCode()) {
            log.error("Milvus search failed: {}", response.getMessage());
            return Collections.emptyList();
        }

        SearchResultsWrapper wrapper = new SearchResultsWrapper(response.getData().getResults());
        List<SearchResult> results = new ArrayList<>();
        List<SearchResultsWrapper.IDScore> scores = wrapper.getIDScore(0);
        for (SearchResultsWrapper.IDScore score : scores) {
            String content = (String) score.get("content");
            String docId = (String) score.get("document_id");
            Long chunkIdx = ((Number) score.get("chunk_index")).longValue();
            String metadataJson = (String) score.get("metadata");
            @SuppressWarnings("unchecked")
            Map<String, Object> metadata = metadataJson != null
                    ? gson.fromJson(metadataJson, Map.class)
                    : Collections.emptyMap();
            results.add(new SearchResult(content, score.getScore(), docId, chunkIdx, metadata));
        }
        return results;
    }

    private String getCollectionName(Long knowledgeBaseId) {
        return milvusProperties.getCollectionPrefix() + knowledgeBaseId;
    }

    private boolean collectionExists(String collectionName) {
        R<Boolean> response = milvusServiceClient.hasCollection(
                HasCollectionParam.newBuilder().withCollectionName(collectionName).build());
        return response.getData();
    }

    private void dropCollection(String collectionName) {
        milvusServiceClient.releaseCollection(
                ReleaseCollectionParam.newBuilder().withCollectionName(collectionName).build());
        milvusServiceClient.dropCollection(
                DropCollectionParam.newBuilder().withCollectionName(collectionName).build());
        log.info("Dropped Milvus collection: {}", collectionName);
    }

    private boolean isDimensionMatched(String collectionName) {
        try {
            R<DescribeCollectionResponse> response = milvusServiceClient.describeCollection(
                    DescribeCollectionParam.newBuilder().withCollectionName(collectionName).build());
            if (response.getStatus() != R.Status.Success.getCode()) {
                return false;
            }
            return response.getData().getSchema().getFieldsList().stream()
                    .filter(f -> f.getName().equals("vector"))
                    .findFirst()
                    .map(f -> f.getTypeParamsList().stream()
                            .filter(p -> p.getKey().equals("dim"))
                            .findFirst()
                            .map(p -> String.valueOf(milvusProperties.getDimension()).equals(p.getValue()))
                            .orElse(false))
                    .orElse(false);
        } catch (Exception e) {
            log.warn("Failed to check dimension for collection: {}", collectionName, e);
            return false;
        }
    }

    private void ensureCollection(String collectionName) {
        if (collectionExists(collectionName)) {
            if (!isDimensionMatched(collectionName)) {
                log.warn("Dimension mismatch for collection: {}, dropping and recreating", collectionName);
                dropCollection(collectionName);
            } else {
                return;
            }
        }

        FieldType idField = FieldType.newBuilder()
                .withName("id")
                .withDataType(DataType.Int64)
                .withPrimaryKey(true)
                .withAutoID(true)
                .build();
        FieldType contentField = FieldType.newBuilder()
                .withName("content")
                .withDataType(DataType.VarChar)
                .withMaxLength(65535)
                .build();
        FieldType vectorField = FieldType.newBuilder()
                .withName("vector")
                .withDataType(DataType.FloatVector)
                .withDimension(milvusProperties.getDimension())
                .build();
        FieldType docIdField = FieldType.newBuilder()
                .withName("document_id")
                .withDataType(DataType.VarChar)
                .withMaxLength(100)
                .build();
        FieldType chunkIdxField = FieldType.newBuilder()
                .withName("chunk_index")
                .withDataType(DataType.Int64)
                .build();
        FieldType metadataField = FieldType.newBuilder()
                .withName("metadata")
                .withDataType(DataType.VarChar)
                .withMaxLength(65535)
                .build();

        CollectionSchemaParam schema = CollectionSchemaParam.newBuilder()
                .addFieldType(idField)
                .addFieldType(contentField)
                .addFieldType(vectorField)
                .addFieldType(docIdField)
                .addFieldType(chunkIdxField)
                .addFieldType(metadataField)
                .build();

        CreateCollectionParam createParam = CreateCollectionParam.newBuilder()
                .withCollectionName(collectionName)
                .withSchema(schema)
                .build();
        milvusServiceClient.createCollection(createParam);

        milvusServiceClient.createIndex(CreateIndexParam.newBuilder()
                .withCollectionName(collectionName)
                .withFieldName("vector")
                .withIndexType(IndexType.HNSW)
                .withMetricType(MetricType.COSINE)
                .withExtraParam("{\"M\":16,\"efConstruction\":256}")
                .build());

        milvusServiceClient.loadCollection(LoadCollectionParam.newBuilder()
                .withCollectionName(collectionName)
                .build());

        log.info("Created Milvus collection: {} with dimension: {}", collectionName, milvusProperties.getDimension());
    }
}
