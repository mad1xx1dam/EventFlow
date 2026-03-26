package com.eventflow.eventflow_backend.config.properties;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "app.minio")
@Getter
@Setter
public class MinioProperties {

    private String publicBaseUrl;
    private String endpoint;
    private String accessKey;
    private String secretKey;
    private String bucket;

    public String getPublicBaseUrl() {
        return publicBaseUrl + "/" + bucket;
    }
}