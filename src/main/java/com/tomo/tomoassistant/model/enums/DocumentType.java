package com.tomo.tomoassistant.model.enums;

import lombok.Getter;
import lombok.AllArgsConstructor;

@Getter
@AllArgsConstructor
public enum DocumentType {
    PDF("pdf"),
    DOCX("docx"),
    DOC("doc"),
    TXT("txt"),
    MD("md"),
    IMAGE("image");

    private final String extension;

    public static DocumentType fromFileName(String fileName) {
        String ext = fileName.substring(fileName.lastIndexOf('.') + 1).toLowerCase();
        return switch (ext) {
            case "pdf" -> PDF;
            case "docx" -> DOCX;
            case "doc" -> DOC;
            case "txt" -> TXT;
            case "md" -> MD;
            case "jpg", "jpeg", "png", "gif", "bmp", "webp" -> IMAGE;
            default -> throw new IllegalArgumentException("Unsupported file type: " + ext);
        };
    }

    public boolean isImage() {
        return this == IMAGE;
    }
}
