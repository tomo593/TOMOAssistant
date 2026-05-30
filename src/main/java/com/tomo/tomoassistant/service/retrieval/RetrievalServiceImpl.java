package com.tomo.tomoassistant.service.retrieval;

import com.tomo.tomoassistant.config.properties.RagProperties;
import com.tomo.tomoassistant.repository.DocumentRepository;
import com.tomo.tomoassistant.service.embedding.EmbeddingService;
import com.tomo.tomoassistant.service.embedding.EmbeddingService.SearchResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class RetrievalServiceImpl implements RetrievalService {

    private final EmbeddingService embeddingService;
    private final DocumentRepository documentRepository;
    private final RagProperties ragProperties;

    @Override
    public List<RetrievalResult> retrieve(String query, Long knowledgeBaseId, int topK) {
        if (knowledgeBaseId == null) {
            return Collections.emptyList();
        }

        try {
            // Embed the query
            float[] queryVector = embeddingService.embedQuery(query);

            // Search in Milvus
            List<SearchResult> results = embeddingService.search(knowledgeBaseId, queryVector, topK);

            // Convert to RetrievalResult and enrich with document info
            return results.stream()
                    .map(r -> {
                        String docName = getDocumentName(r.documentId());
                        return new RetrievalResult(
                                r.content(),
                                r.score(),
                                r.documentId(),
                                docName,
                                r.chunkIndex(),
                                r.metadata()
                        );
                    })
                    .toList();
        } catch (Exception e) {
            log.error("Retrieval failed for query: {}", query, e);
            return Collections.emptyList();
        }
    }

    private String getDocumentName(String documentId) {
        try {
            return documentRepository.findById(Long.parseLong(documentId))
                    .map(doc -> doc.getOriginalName())
                    .orElse("Unknown");
        } catch (Exception e) {
            return "Unknown";
        }
    }
}
