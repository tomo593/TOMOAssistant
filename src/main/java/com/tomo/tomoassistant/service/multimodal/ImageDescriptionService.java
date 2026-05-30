package com.tomo.tomoassistant.service.multimodal;

public interface ImageDescriptionService {
    String generateDescription(byte[] imageData, String fileName);
}
