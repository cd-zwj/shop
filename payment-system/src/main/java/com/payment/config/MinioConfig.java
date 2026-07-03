package com.payment.config;

import io.minio.MinioClient;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * MinIO配置
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "minio")
public class MinioConfig {
    
    /**
     * MinIO服务地址
     */
    private String endpoint;
    
    /**
     * 访问密钥
     */
    private String accessKey;
    
    /**
     * 密钥
     */
    private String secretKey;
    
    /**
     * 存储桶名称
     */
    private String bucketName;

    /**
     * 文件路径前缀
     */
    private String pathPrefix = "uploads/";
    
    /**
     * 预签名URL有效期（天）
     */
    private Integer urlExpiryDays = 7;
    
    /**
     * 创建 MinIO 客户端 Bean。
     *
     * @return MinioClient 实例，配置了连接地址和访问凭证
     */
    @Bean
    public MinioClient minioClient() {
        return MinioClient.builder()
                .endpoint(endpoint)
                .credentials(accessKey, secretKey)
                .build();
    }
}
