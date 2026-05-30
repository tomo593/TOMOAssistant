package com.tomo.tomoassistant.model.dto;

import lombok.Data;

@Data
public class ChatRequest {
    private Long conversationId;
    private String message;
    private Long knowledgeBaseId;
    private String modelName;
    private boolean stream = true;
    private String imageData;
    private String imageName;
}
