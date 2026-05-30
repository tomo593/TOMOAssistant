package com.tomo.tomoassistant.service.document;

import lombok.extern.slf4j.Slf4j;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.metadata.TikaCoreProperties;
import org.apache.tika.parser.AutoDetectParser;
import org.apache.tika.parser.ParseContext;
import org.apache.tika.sax.BodyContentHandler;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
public class TikaDocumentParser implements DocumentParser {

    private final AutoDetectParser parser = new AutoDetectParser();

    @Override
    public List<DocumentSegment> parse(InputStream inputStream, String fileName) {
        try {
            Metadata metadata = new Metadata();
            metadata.set(TikaCoreProperties.RESOURCE_NAME_KEY, fileName);
            BodyContentHandler handler = new BodyContentHandler(-1);
            ParseContext context = new ParseContext();
            context.set(AutoDetectParser.class, parser);

            parser.parse(inputStream, handler, metadata, context);

            String fullText = handler.toString();
            if (fullText.isBlank()) {
                return List.of();
            }

            List<DocumentSegment> segments = new ArrayList<>();

            String[] paragraphs = fullText.split("\\n\\s*\\n");
            for (int i = 0; i < paragraphs.length; i++) {
                String content = paragraphs[i].trim();
                if (!content.isEmpty()) {
                    Map<String, Object> segMeta = new HashMap<>();
                    segMeta.put("paragraph_index", i);
                    segMeta.put("source", fileName);
                    segMeta.put("title", metadata.get(TikaCoreProperties.TITLE));
                    segments.add(new DocumentSegment(content, segMeta));
                }
            }

            log.info("Parsed {} segments from file: {}", segments.size(), fileName);
            return segments;
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse document: " + fileName, e);
        }
    }
}
