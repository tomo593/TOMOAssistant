package com.tomo.tomoassistant.controller;

import com.tomo.tomoassistant.common.result.Result;
import com.tomo.tomoassistant.model.dto.ChatRequest;
import com.tomo.tomoassistant.service.chat.StreamingChatService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
public class ChatController {

    private final StreamingChatService streamingChatService;

    @PostMapping("/stream")
    public SseEmitter stream(@RequestBody @Valid ChatRequest request) {
        return streamingChatService.streamChat(request);
    }
}
