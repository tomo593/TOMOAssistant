package com.tomo.tomoassistant.service.storage;

import com.tomo.tomoassistant.config.properties.MinioProperties;
import io.minio.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class MinioStorageServiceImpl implements MinioStorageService {

    private final MinioClient minioClient;
    private final MinioProperties minioProperties;

    @Override
    public String uploadFile(MultipartFile file, String objectKey) {
        try {
            minioClient.putObject(PutObjectArgs.builder()
                    .bucket(minioProperties.getBucketName())
                    .object(objectKey)
                    .stream(file.getInputStream(), file.getSize(), -1)
                    .contentType(file.getContentType())
                    .build());
            log.info("Uploaded file to MinIO: {}", objectKey);
            return objectKey;
        } catch (Exception e) {
            throw new RuntimeException("Failed to upload file to MinIO", e);
        }
    }

    @Override
    public void deleteFile(String objectKey) {
        try {
            minioClient.removeObject(RemoveObjectArgs.builder()
                    .bucket(minioProperties.getBucketName())
                    .object(objectKey)
                    .build());
            log.info("Deleted file from MinIO: {}", objectKey);
        } catch (Exception e) {
            throw new RuntimeException("Failed to delete file from MinIO", e);
        }
    }

    @Override
    public String getPresignedUrl(String objectKey, int expirySeconds) {
        try {
            return minioClient.getPresignedObjectUrl(GetPresignedObjectUrlArgs.builder()
                    .bucket(minioProperties.getBucketName())
                    .object(objectKey)
                    .expiry(expirySeconds)
                    .build());
        } catch (Exception e) {
            throw new RuntimeException("Failed to get presigned URL", e);
        }
    }

    @Override
    public String generateObjectKey(Long knowledgeBaseId, String originalName) {
        String ext = originalName.contains(".")
                ? originalName.substring(originalName.lastIndexOf('.'))
                : "";
        return "kb/" + knowledgeBaseId + "/" + UUID.randomUUID() + ext;
    }

    @Override
    public byte[] downloadFile(String objectKey) {
        try (var stream = minioClient.getObject(GetObjectArgs.builder()
                .bucket(minioProperties.getBucketName())
                .object(objectKey)
                .build())) {
            return stream.readAllBytes();
        } catch (Exception e) {
            throw new RuntimeException("Failed to download file from MinIO", e);
        }
    }
}
