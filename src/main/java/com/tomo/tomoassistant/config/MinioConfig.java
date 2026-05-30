package com.tomo.tomoassistant.config;

import com.tomo.tomoassistant.config.properties.MinioProperties;
import io.minio.BucketExistsArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class MinioConfig {

    private final MinioProperties minioProperties;

    @Bean
    public MinioClient minioClient() {
        return MinioClient.builder()
                .endpoint(minioProperties.getEndpoint())
                .credentials(minioProperties.getAccessKey(), minioProperties.getSecretKey())
                .build();
    }

    @Bean
    public ApplicationRunner minioBucketInitializer(MinioClient minioClient) {
        return args -> {
            try {
                boolean exists = minioClient.bucketExists(
                        BucketExistsArgs.builder().bucket(minioProperties.getBucketName()).build());
                if (!exists) {
                    minioClient.makeBucket(
                            MakeBucketArgs.builder().bucket(minioProperties.getBucketName()).build());
                    log.info("Created MinIO bucket: {}", minioProperties.getBucketName());
                }
            } catch (Exception e) {
                log.warn("Failed to initialize MinIO bucket: {}", e.getMessage());
            }
        };
    }
}
