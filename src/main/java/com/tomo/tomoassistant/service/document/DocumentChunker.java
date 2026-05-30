package com.tomo.tomoassistant.service.document;

import com.tomo.tomoassistant.service.document.DocumentParser.DocumentSegment;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
public class DocumentChunker {

    public record TextChunk(String content, int startIndex, int endIndex, Map<String, Object> metadata) {}

    public List<TextChunk> chunk(List<DocumentSegment> segments, int chunkSize, int chunkOverlap) {
        // First merge all segment text
        StringBuilder fullText = new StringBuilder();
        List<int[]> segmentBoundaries = new ArrayList<>();
        for (DocumentSegment segment : segments) {
            int start = fullText.length();
            fullText.append(segment.content()).append("\n\n");
            int end = fullText.length();
            segmentBoundaries.add(new int[]{start, end});
        }

        String text = fullText.toString().trim();
        if (text.isEmpty()) {
            return List.of();
        }

        // Split into chunks with overlap
        List<TextChunk> chunks = new ArrayList<>();
        int start = 0;
        int index = 0;

        while (start < text.length()) {
            int end = Math.min(start + chunkSize, text.length());

            // Try to break at sentence or word boundary
            if (end < text.length()) {
                int lastPeriod = text.lastIndexOf('。', end);
                int lastNewline = text.lastIndexOf('\n', end);
                int breakPoint = Math.max(lastPeriod, lastNewline);
                if (breakPoint > start + chunkSize / 2) {
                    end = breakPoint + 1;
                }
            }

            String content = text.substring(start, end).trim();
            if (!content.isEmpty()) {
                Map<String, Object> metadata = new HashMap<>();
                metadata.put("chunk_index", index);
                metadata.put("start_offset", start);
                metadata.put("end_offset", end);

                // Find which segments this chunk belongs to
                for (int i = 0; i < segmentBoundaries.size(); i++) {
                    int[] boundary = segmentBoundaries.get(i);
                    if (start < boundary[1] && end > boundary[0]) {
                        metadata.put("paragraph_index", i);
                        DocumentSegment seg = segments.get(i);
                        seg.metadata().forEach((k, v) -> {
                            if (!metadata.containsKey(k)) metadata.put(k, v);
                        });
                        break;
                    }
                }

                chunks.add(new TextChunk(content, start, end, metadata));
                index++;
            }

            start = end - chunkOverlap;
            if (start >= text.length() || start < 0) break;
        }

        log.info("Created {} chunks from text of length {}", chunks.size(), text.length());
        return chunks;
    }
}
