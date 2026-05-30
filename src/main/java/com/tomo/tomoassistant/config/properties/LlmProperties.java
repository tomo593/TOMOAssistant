package com.tomo.tomoassistant.config.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "tomo.llm")
public class LlmProperties {

    private ModelConfig chat = new ModelConfig();
    private ModelConfig embedding = new ModelConfig();
    private ModelConfig multimodal = new ModelConfig();
    private OllamaConfig ollama = new OllamaConfig();

    @Data
    public static class ModelConfig {
        private String baseUrl = "https://api.openai.com/v1";
        private String apiKey;
        private String modelName;
        private double temperature = 0.7;
        private int maxTokens = 4096;
        private int dimension = 2048;
    }

    @Data
    public static class OllamaConfig {
        private String baseUrl = "http://localhost:11434/v1";
        private String apiKey = "ollama";
        private String modelName = "qwen2.5:7b";
        private String embeddingModelName = "nomic-embed-text";
        private String multimodalModelName = "llava:7b";
        private double temperature = 0.7;
        private int maxTokens = 4096;
        private int dimension = 2048;
    }
}
