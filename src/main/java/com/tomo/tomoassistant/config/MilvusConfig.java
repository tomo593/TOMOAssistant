package com.tomo.tomoassistant.config;

import com.tomo.tomoassistant.config.properties.MilvusProperties;
import io.milvus.client.MilvusServiceClient;
import io.milvus.param.ConnectParam;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class MilvusConfig {

    private final MilvusProperties milvusProperties;

    @Bean
    public MilvusServiceClient milvusServiceClient() {
        ConnectParam connectParam = ConnectParam.newBuilder()
                .withUri(milvusProperties.getUri())
                .build();
        log.info("Connecting to Milvus at: {}", milvusProperties.getUri());
        return new MilvusServiceClient(connectParam);
    }
}
