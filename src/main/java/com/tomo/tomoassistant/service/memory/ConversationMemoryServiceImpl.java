package com.tomo.tomoassistant.service.memory;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tomo.tomoassistant.model.entity.MessageEntity;
import com.tomo.tomoassistant.model.enums.MessageRole;
import com.tomo.tomoassistant.repository.MessageRepository;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class ConversationMemoryServiceImpl implements ConversationMemoryService {

    private final MessageRepository messageRepository;
    private final RedisTemplate<String, Object> redisTemplate;
    private final ObjectMapper objectMapper;

    private static final String REDIS_KEY_PREFIX = "chat:history:";
    private static final long CACHE_TTL_HOURS = 24;

    @Override
    public List<ChatMessage> getRecentMessages(Long conversationId, int maxMessages) {
        String redisKey = REDIS_KEY_PREFIX + conversationId;

        // Try Redis cache first
        try {
            Object cached = redisTemplate.opsForValue().get(redisKey);
            if (cached instanceof List<?> list && !list.isEmpty()) {
                return convertToChatMessages(list);
            }
        } catch (Exception e) {
            log.warn("Redis cache read failed: {}", e.getMessage());
        }

        // Fallback to DB
        List<MessageEntity> messages = messageRepository.findTop20ByConversationIdOrderByCreatedAtDesc(conversationId);
        Collections.reverse(messages);

        // Cache in Redis
        try {
            redisTemplate.opsForValue().set(redisKey, messages, CACHE_TTL_HOURS, TimeUnit.HOURS);
        } catch (Exception e) {
            log.warn("Redis cache write failed: {}", e.getMessage());
        }

        return messages.stream()
                .map(this::toChatMessage)
                .toList();
    }

    @Override
    public void addMessage(Long conversationId, MessageRole role, String content, String metadataJson) {
        MessageEntity message = new MessageEntity();
        message.setConversationId(conversationId);
        message.setRole(role.name());
        message.setContent(content);
        message.setMetadataJson(metadataJson);
        message.setCreatedAt(LocalDateTime.now());
        messageRepository.save(message);

        // Evict cache so next read gets fresh data
        evictCache(conversationId);
    }

    @Override
    public void evictCache(Long conversationId) {
        try {
            redisTemplate.delete(REDIS_KEY_PREFIX + conversationId);
        } catch (Exception e) {
            log.warn("Redis cache eviction failed: {}", e.getMessage());
        }
    }

    private ChatMessage toChatMessage(MessageEntity entity) {
        return switch (MessageRole.valueOf(entity.getRole())) {
            case USER -> UserMessage.from(entity.getContent());
            case ASSISTANT -> AiMessage.from(entity.getContent());
            case SYSTEM -> SystemMessage.from(entity.getContent());
           
        };
    }

    @SuppressWarnings("unchecked")
    private List<ChatMessage> convertToChatMessages(List<?> list) {
        List<ChatMessage> messages = new ArrayList<>();
        for (Object item : list) {
            if (item instanceof MessageEntity entity) {
                messages.add(toChatMessage(entity));
            }
        }
        return messages;
    }
}
