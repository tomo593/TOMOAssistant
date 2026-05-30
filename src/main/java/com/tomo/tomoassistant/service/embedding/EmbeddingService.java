package com.tomo.tomoassistant.service.embedding;

import java.util.List;
import java.util.Map;

public interface EmbeddingService {
    List<float[]> embed(List<String> texts);
    float[] embedQuery(String text);
    void storeEmbeddings(Long knowledgeBaseId, List<EmbeddedChunk> chunks);
    void deleteEmbeddings(Long knowledgeBaseId, String documentId);
    List<SearchResult> search(Long knowledgeBaseId, float[] queryVector, int topK);

    record EmbeddedChunk(String content, float[] vector, Map<String, Object> metadata, String documentId, Long chunkIndex) {}
    record SearchResult(String content, float score, String documentId, Long chunkIndex, Map<String, Object> metadata) {}
}
