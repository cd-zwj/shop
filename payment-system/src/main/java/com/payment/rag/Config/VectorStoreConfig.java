package com.payment.rag.Config;

import io.micrometer.observation.ObservationRegistry;
import io.milvus.client.MilvusServiceClient;
import io.milvus.param.ConnectParam;
import io.milvus.param.IndexType;
import io.milvus.param.MetricType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.milvus.MilvusVectorStore;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.util.StringUtils;

import java.util.concurrent.TimeUnit;

@Slf4j
@Configuration
@ConditionalOnProperty(name = "spring.ai.vectorstore.milvus.enabled", havingValue = "true", matchIfMissing = true)
public class VectorStoreConfig {

    @Bean
    public MilvusServiceClient milvusServiceClient(
            @Value("${spring.ai.vectorstore.milvus.client.host:localhost}") String host,
            @Value("${spring.ai.vectorstore.milvus.client.port:19530}") int port,
            @Value("${spring.ai.vectorstore.milvus.client.uri:}") String uri,
            @Value("${spring.ai.vectorstore.milvus.client.username:}") String username,
            @Value("${spring.ai.vectorstore.milvus.client.password:}") String password,
            @Value("${spring.ai.vectorstore.milvus.client.token:}") String token,
            @Value("${spring.ai.vectorstore.milvus.client.secure:false}") boolean secure,
            @Value("${spring.ai.vectorstore.milvus.database-name:default}") String databaseName,
            @Value("${spring.ai.vectorstore.milvus.client.connect-timeout-ms:10000}") long connectTimeoutMs,
            @Value("${spring.ai.vectorstore.milvus.client.keep-alive-time-ms:55000}") long keepAliveTimeMs,
            @Value("${spring.ai.vectorstore.milvus.client.keep-alive-timeout-ms:20000}") long keepAliveTimeoutMs,
            @Value("${spring.ai.vectorstore.milvus.client.rpc-deadline-ms:60000}") long rpcDeadlineMs) {

        ConnectParam.Builder builder = ConnectParam.newBuilder()
                .withDatabaseName(databaseName)
                .withConnectTimeout(connectTimeoutMs, TimeUnit.MILLISECONDS)
                .withKeepAliveTime(keepAliveTimeMs, TimeUnit.MILLISECONDS)
                .withKeepAliveTimeout(keepAliveTimeoutMs, TimeUnit.MILLISECONDS)
                .withRpcDeadline(rpcDeadlineMs, TimeUnit.MILLISECONDS)
                .withSecure(secure);

        if (StringUtils.hasText(uri)) {
            builder.withUri(uri);
        } else {
            builder.withHost(host).withPort(port);
        }

        if (StringUtils.hasText(token)) {
            builder.withToken(token);
        } else if (StringUtils.hasText(username) || StringUtils.hasText(password)) {
            builder.withAuthorization(username, password);
        }

        log.info("Milvus vector store client configured: endpoint={}, database={}",
                StringUtils.hasText(uri) ? uri : host + ":" + port, databaseName);
        return new MilvusServiceClient(builder.build());
    }

    @Bean
    @Primary
    public VectorStore leafVectorStore(EmbeddingModel embeddingModel,
                                       MilvusServiceClient milvusServiceClient,
                                       @Value("${spring.ai.vectorstore.milvus.initialize-schema:true}") boolean initializeSchema,
                                       @Value("${spring.ai.vectorstore.milvus.database-name:default}") String databaseName,
                                       @Value("${spring.ai.vectorstore.milvus.leaf-collection-name:rag_leaf_vectors}") String collectionName,
                                       @Value("${spring.ai.vectorstore.milvus.embedding-dimension:0}") int embeddingDimension,
                                       @Value("${spring.ai.vectorstore.milvus.index-type:IVF_FLAT}") String indexType,
                                       @Value("${spring.ai.vectorstore.milvus.metric-type:COSINE}") String metricType,
                                       @Value("${spring.ai.vectorstore.milvus.index-parameters:}") String indexParameters,
                                       ObjectProvider<ObservationRegistry> observationRegistryProvider) {
        return buildMilvusVectorStore(
                embeddingModel,
                milvusServiceClient,
                initializeSchema,
                databaseName,
                collectionName,
                embeddingDimension,
                indexType,
                metricType,
                indexParameters,
                observationRegistryProvider);
    }

    @Bean
    public VectorStore summaryVectorStore(EmbeddingModel embeddingModel,
                                          MilvusServiceClient milvusServiceClient,
                                          @Value("${spring.ai.vectorstore.milvus.initialize-schema:true}") boolean initializeSchema,
                                          @Value("${spring.ai.vectorstore.milvus.database-name:default}") String databaseName,
                                          @Value("${spring.ai.vectorstore.milvus.summary-collection-name:rag_summary_vectors}") String collectionName,
                                          @Value("${spring.ai.vectorstore.milvus.embedding-dimension:0}") int embeddingDimension,
                                          @Value("${spring.ai.vectorstore.milvus.index-type:IVF_FLAT}") String indexType,
                                          @Value("${spring.ai.vectorstore.milvus.metric-type:COSINE}") String metricType,
                                          @Value("${spring.ai.vectorstore.milvus.index-parameters:}") String indexParameters,
                                          ObjectProvider<ObservationRegistry> observationRegistryProvider) {
        return buildMilvusVectorStore(
                embeddingModel,
                milvusServiceClient,
                initializeSchema,
                databaseName,
                collectionName,
                embeddingDimension,
                indexType,
                metricType,
                indexParameters,
                observationRegistryProvider);
    }

    private VectorStore buildMilvusVectorStore(EmbeddingModel embeddingModel,
                                               MilvusServiceClient milvusServiceClient,
                                               boolean initializeSchema,
                                               String databaseName,
                                               String collectionName,
                                               int embeddingDimension,
                                               String indexType,
                                               String metricType,
                                               String indexParameters,
                                               ObjectProvider<ObservationRegistry> observationRegistryProvider) {
        MilvusVectorStore.Builder builder = MilvusVectorStore.builder(milvusServiceClient, embeddingModel)
                .initializeSchema(initializeSchema)
                .databaseName(databaseName)
                .collectionName(collectionName)
                .indexType(IndexType.valueOf(indexType))
                .metricType(MetricType.valueOf(metricType))
                .observationRegistry(observationRegistryProvider.getIfAvailable(() -> ObservationRegistry.NOOP));

        if (embeddingDimension > 0) {
            builder.embeddingDimension(embeddingDimension);
        }
        if (StringUtils.hasText(indexParameters)) {
            builder.indexParameters(indexParameters);
        }

        log.info("Milvus vector store configured: collection={}, initializeSchema={}, indexType={}, metricType={}",
                collectionName, initializeSchema, indexType, metricType);
        return builder.build();
    }
}
