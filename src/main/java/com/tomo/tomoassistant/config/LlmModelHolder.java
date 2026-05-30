package com.tomo.tomoassistant.config;

import com.tomo.tomoassistant.config.properties.LlmProperties;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.chat.StreamingChatLanguageModel;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.model.openai.OpenAiEmbeddingModel;
import dev.langchain4j.model.openai.OpenAiStreamingChatModel;
import jakarta.annotation.PostConstruct;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

@Slf4j
@Component
public class LlmModelHolder {

    private static final String REDIS_KEY = "tomo:llm:mode";
    private static final String MODE_ONLINE = "online";
    private static final String MODE_OLLAMA = "ollama";

    private final LlmProperties llmProperties;
    private final RedisTemplate<String, Object> redisTemplate;

    @Getter
    private volatile String currentMode;

    @Getter
    private volatile ChatLanguageModel chatModel;
    @Getter
    private volatile StreamingChatLanguageModel streamingChatModel;
    @Getter
    private volatile EmbeddingModel embeddingModel;
    @Getter
    private volatile ChatLanguageModel multimodalModel;
    @Getter
    private volatile StreamingChatLanguageModel streamingMultimodalModel;

    public LlmModelHolder(LlmProperties llmProperties, RedisTemplate<String, Object> redisTemplate) {
        this.llmProperties = llmProperties;
        this.redisTemplate = redisTemplate;
    }

    @PostConstruct
    public void init() {
        String savedMode = (String) redisTemplate.opsForValue().get(REDIS_KEY);
        if (savedMode == null) {
            savedMode = MODE_ONLINE;
        }
        this.currentMode = savedMode;
        rebuildModels();
        log.info("LLM model holder initialized, mode: {}", currentMode);
    }

    public synchronized void switchMode(String mode) {
        if (!MODE_ONLINE.equals(mode) && !MODE_OLLAMA.equals(mode)) {
            throw new IllegalArgumentException("Invalid mode: " + mode + ". Must be 'online' or 'ollama'");
        }
        if (mode.equals(this.currentMode)) {
            return;
        }
        this.currentMode = mode;
        redisTemplate.opsForValue().set(REDIS_KEY, mode, 365, TimeUnit.DAYS);
        rebuildModels();
        log.info("LLM mode switched to: {}", mode);
    }

    private void rebuildModels() {
        if (MODE_OLLAMA.equals(currentMode)) {
            buildOllamaModels();
        } else {
            buildOnlineModels();
        }
    }

    private void buildOnlineModels() {
        LlmProperties.ModelConfig chatCfg = llmProperties.getChat();
        this.chatModel = OpenAiChatModel.builder()
                .baseUrl(chatCfg.getBaseUrl())
                .apiKey(chatCfg.getApiKey())
                .modelName(chatCfg.getModelName())
                .temperature(chatCfg.getTemperature())
                .maxTokens(chatCfg.getMaxTokens())
                .build();
        this.streamingChatModel = OpenAiStreamingChatModel.builder()
                .baseUrl(chatCfg.getBaseUrl())
                .apiKey(chatCfg.getApiKey())
                .modelName(chatCfg.getModelName())
                .temperature(chatCfg.getTemperature())
                .maxTokens(chatCfg.getMaxTokens())
                .build();

        LlmProperties.ModelConfig embedCfg = llmProperties.getEmbedding();
        this.embeddingModel = OpenAiEmbeddingModel.builder()
                .baseUrl(embedCfg.getBaseUrl())
                .apiKey(embedCfg.getApiKey())
                .modelName(embedCfg.getModelName())
                .dimensions(embedCfg.getDimension())
                .build();

        LlmProperties.ModelConfig multiCfg = llmProperties.getMultimodal();
        this.multimodalModel = OpenAiChatModel.builder()
                .baseUrl(multiCfg.getBaseUrl())
                .apiKey(multiCfg.getApiKey())
                .modelName(multiCfg.getModelName())
                .temperature(multiCfg.getTemperature())
                .build();
        this.streamingMultimodalModel = OpenAiStreamingChatModel.builder()
                .baseUrl(multiCfg.getBaseUrl())
                .apiKey(multiCfg.getApiKey())
                .modelName(multiCfg.getModelName())
                .temperature(multiCfg.getTemperature())
                .maxTokens(multiCfg.getMaxTokens())
                .build();

        log.info("Built online LLM models: chat={}, embedding={}, multimodal={}",
                chatCfg.getModelName(), embedCfg.getModelName(), multiCfg.getModelName());
    }

    private void buildOllamaModels() {
        LlmProperties.OllamaConfig cfg = llmProperties.getOllama();

        this.chatModel = OpenAiChatModel.builder()
                .baseUrl(cfg.getBaseUrl())
                .apiKey(cfg.getApiKey())
                .modelName(cfg.getModelName())
                .temperature(cfg.getTemperature())
                .maxTokens(cfg.getMaxTokens())
                .build();
        this.streamingChatModel = OpenAiStreamingChatModel.builder()
                .baseUrl(cfg.getBaseUrl())
                .apiKey(cfg.getApiKey())
                .modelName(cfg.getModelName())
                .temperature(cfg.getTemperature())
                .maxTokens(cfg.getMaxTokens())
                .build();

        this.embeddingModel = OpenAiEmbeddingModel.builder()
                .baseUrl(cfg.getBaseUrl())
                .apiKey(cfg.getApiKey())
                .modelName(cfg.getEmbeddingModelName())
                .dimensions(cfg.getDimension())
                .build();

        this.multimodalModel = OpenAiChatModel.builder()
                .baseUrl(cfg.getBaseUrl())
                .apiKey(cfg.getApiKey())
                .modelName(cfg.getMultimodalModelName())
                .temperature(cfg.getTemperature())
                .build();
        this.streamingMultimodalModel = OpenAiStreamingChatModel.builder()
                .baseUrl(cfg.getBaseUrl())
                .apiKey(cfg.getApiKey())
                .modelName(cfg.getMultimodalModelName())
                .temperature(cfg.getTemperature())
                .maxTokens(cfg.getMaxTokens())
                .build();

        log.info("Built Ollama LLM models: chat={}, embedding={}, multimodal={}",
                cfg.getModelName(), cfg.getEmbeddingModelName(), cfg.getMultimodalModelName());
    }
}
