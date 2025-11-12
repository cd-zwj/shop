package com.payment.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * 阿里云OSS配置
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "aliyun.oss")
public class OssConfig {
    
    private String endpoint;
    
    private String accessKeyId;
    
    private String accessKeySecret;
    
    private String bucketName;
    
    private String domain;
    
    private String pathPrefix;
}

