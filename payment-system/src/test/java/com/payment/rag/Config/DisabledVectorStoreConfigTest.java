package com.payment.rag.Config;

import io.milvus.client.MilvusServiceClient;
import org.junit.jupiter.api.Test;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;

class DisabledVectorStoreConfigTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withUserConfiguration(DisabledVectorStoreConfig.class, VectorStoreConfig.class);

    @Test
    void disabledVectorStoresShouldBeAvailableWhenMilvusIsNotExplicitlyEnabled() {
        contextRunner.run(context -> {
            assertThat(context).doesNotHaveBean(MilvusServiceClient.class);
            assertThat(context).hasBean("leafVectorStore");
            assertThat(context).hasBean("summaryVectorStore");

            VectorStore leafVectorStore = context.getBean("leafVectorStore", VectorStore.class);
            assertThat(leafVectorStore.getName()).isEqualTo("leafVectorStore");
            assertThat(leafVectorStore.similaritySearch(SearchRequest.builder().query("local").topK(3).build())).isEmpty();
        });
    }

    @Test
    void disabledVectorStoresShouldBackOffWhenMilvusIsEnabled() {
        new ApplicationContextRunner()
                .withUserConfiguration(DisabledVectorStoreConfig.class)
                .withPropertyValues("spring.ai.vectorstore.milvus.enabled=true")
                .run(context -> {
                    assertThat(context).doesNotHaveBean("leafVectorStore");
                    assertThat(context).doesNotHaveBean("summaryVectorStore");
                });
    }
}
