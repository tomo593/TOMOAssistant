package com.tomo.tomoassistant.service.chat;

import com.tomo.tomoassistant.model.dto.ChatRequest;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

public interface StreamingChatService {
    SseEmitter streamChat(ChatRequest request);
}
