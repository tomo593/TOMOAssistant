package com.tomo.tomoassistant.model.vo;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class MessageVO {
    private Long id;
    private String role;
    private String content;
    private List<CitationVO> citations;
    private LocalDateTime createdAt;
    private String imageData;
    private String imageName;
}
