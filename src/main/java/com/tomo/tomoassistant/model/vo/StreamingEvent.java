package com.tomo.tomoassistant.model.vo;

import com.tomo.tomoassistant.model.enums.StreamingEventType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StreamingEvent {
    private StreamingEventType type;
    private String content;
    private CitationVO citation;
    private String title;
    private Long conversationId;
}
