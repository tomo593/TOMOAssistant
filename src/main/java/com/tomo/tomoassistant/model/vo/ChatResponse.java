package com.tomo.tomoassistant.model.vo;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class ChatResponse {
    private String message;
    private List<CitationVO> citations;
}
