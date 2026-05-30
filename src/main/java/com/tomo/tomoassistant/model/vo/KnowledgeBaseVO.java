package com.tomo.tomoassistant.model.vo;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class KnowledgeBaseVO {
    private Long id;
    private String name;
    private String description;
    private String embeddingModel;
    private Integer docCount;
    private Integer status;
    private LocalDateTime createdAt;
}
