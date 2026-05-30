package com.tomo.tomoassistant.service.memory;

import com.tomo.tomoassistant.model.enums.MessageRole;
import dev.langchain4j.data.message.ChatMessage;

import java.util.List;

public interface ConversationMemoryService {
    List<ChatMessage> getRecentMessages(Long conversationId, int maxMessages);
    void addMessage(Long conversationId, MessageRole role, String content, String metadataJson);
    void evictCache(Long conversationId);
}
