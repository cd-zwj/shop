package com.payment.rag.Config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.filter.Filter;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import java.util.List;

@Slf4j
@Configuration
@ConditionalOnProperty(name = "spring.ai.vectorstore.milvus.enabled", havingValue = "false", matchIfMissing = true)
public class DisabledVectorStoreConfig {

    @Bean
    @Primary
    @Qualifier("leafVectorStore")
    public VectorStore leafVectorStore() {
        return new DisabledVectorStore("leafVectorStore");
    }

    @Bean
    @Qualifier("summaryVectorStore")
    public VectorStore summaryVectorStore() {
        return new DisabledVectorStore("summaryVectorStore");
    }

    private static final class DisabledVectorStore implements VectorStore {
        private final String name;

        private DisabledVectorStore(String name) {
            this.name = name;
        }

        @Override
        public String getName() {
            return name;
        }

        @Override
        public void add(List<Document> documents) {
            log.debug("Vector store disabled, ignoring add: store={}, size={}", name, documents == null ? 0 : documents.size());
        }

        @Override
        public void delete(List<String> idList) {
            log.debug("Vector store disabled, ignoring delete: store={}, size={}", name, idList == null ? 0 : idList.size());
        }

        @Override
        public void delete(Filter.Expression filterExpression) {
            log.debug("Vector store disabled, ignoring filtered delete: store={}", name);
        }

        @Override
        public List<Document> similaritySearch(SearchRequest request) {
            return List.of();
        }
    }
}
