package com.tomo.tomoassistant.service.retrieval;

import java.util.List;
import java.util.Map;

public interface RetrievalService {
    List<RetrievalResult> retrieve(String query, Long knowledgeBaseId, int topK);

    record RetrievalResult(
            String content,
            float score,
            String documentId,
            String documentName,
            Long chunkIndex,
            Map<String, Object> metadata
    ) {}
}
