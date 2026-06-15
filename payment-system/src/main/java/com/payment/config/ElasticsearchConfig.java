package com.payment.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.elasticsearch.repository.config.EnableElasticsearchRepositories;

/**
 * Elasticsearch 配置。
 *
 * <p>默认关闭：仅当 {@code app.search.enabled=true} 时才装配 ES 相关 Bean。
 * 未启用时 {@code ProductSearchServiceImpl} 自动降级到 MySQL 模糊查询，
 * 启动阶段不会因为 ES 不可达而持续报错。</p>
 *
 * <p>本地或预生产需要 ES 搜索时：</p>
 * <ul>
 *   <li>在 {@code application-dev.yml} 或环境变量中设置 {@code app.search.enabled=true}</li>
 *   <li>通过 {@code ES_URIS} / {@code ES_USERNAME} / {@code ES_PASSWORD} 指定连接</li>
 * </ul>
 */
@Configuration
@ConditionalOnProperty(name = "app.search.enabled", havingValue = "true", matchIfMissing = false)
@EnableElasticsearchRepositories(basePackages = "com.payment.repository")
public class ElasticsearchConfig {
}
