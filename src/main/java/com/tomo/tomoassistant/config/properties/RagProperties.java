package com.tomo.tomoassistant.config.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "tomo.rag")
public class RagProperties {
    private int chunkSize = 512;
    private int chunkOverlap = 64;
    private int topK = 5;
    private RerankConfig rerank = new RerankConfig();

    @Data
    public static class RerankConfig {
        private boolean enabled = false;
        private String baseUrl;
        private String modelName;
    }
}
