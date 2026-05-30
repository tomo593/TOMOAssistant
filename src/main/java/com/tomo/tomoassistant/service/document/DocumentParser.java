package com.tomo.tomoassistant.service.document;

import java.io.InputStream;
import java.util.List;

public interface DocumentParser {
    List<DocumentSegment> parse(InputStream inputStream, String fileName);

    record DocumentSegment(String content, java.util.Map<String, Object> metadata) {}
}
