package com.tomo.tomoassistant.model.vo;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class DocumentVO {
    private Long id;
    private String fileName;
    private String fileType;
    private Long fileSize;
    private Integer status;
    private Integer chunkCount;
    private String errorMessage;
    private LocalDateTime createdAt;
}
