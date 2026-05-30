package com.tomo.tomoassistant.model.vo;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class CitationVO {
    private Long documentId;
    private String documentName;
    private String chunkContent;
    private Double similarityScore;
    private Integer chunkIndex;
}
