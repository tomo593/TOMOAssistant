package com.tomo.tomoassistant.model.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class KnowledgeBaseCreateRequest {
    @NotBlank(message = "Name cannot be empty")
    private String name;

    private String description;
    private String embeddingModel;
    private Integer chunkSize = 512;
    private Integer chunkOverlap = 64;
}
