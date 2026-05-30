package com.tomo.tomoassistant.config.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "tomo.milvus")
public class MilvusProperties {
    private String uri = "http://localhost:19530";
    private String collectionPrefix = "tomo_kb_";
    private int dimension = 2048;
}
