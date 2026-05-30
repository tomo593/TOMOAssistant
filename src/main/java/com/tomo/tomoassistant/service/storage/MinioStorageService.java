package com.tomo.tomoassistant.service.storage;

import org.springframework.web.multipart.MultipartFile;

public interface MinioStorageService {
    String uploadFile(MultipartFile file, String objectKey);
    void deleteFile(String objectKey);
    String getPresignedUrl(String objectKey, int expirySeconds);
    String generateObjectKey(Long knowledgeBaseId, String originalName);
    byte[] downloadFile(String objectKey);
}
