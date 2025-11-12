package com.payment.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.elasticsearch.repository.config.EnableElasticsearchRepositories;

/**
 * Elasticsearch配置
 */
@Configuration
@EnableElasticsearchRepositories(basePackages = "com.payment.repository")
public class ElasticsearchConfig {
}
