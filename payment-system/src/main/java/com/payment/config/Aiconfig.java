package com.payment.config;

import dev.langchain4j.community.store.embedding.redis.RedisEmbeddingStore;
import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.DocumentSplitter;
import dev.langchain4j.data.document.loader.ClassPathDocumentLoader;
import dev.langchain4j.data.document.splitter.DocumentSplitters;
import dev.langchain4j.memory.ChatMemory;
import dev.langchain4j.memory.chat.ChatMemoryProvider;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.rag.content.retriever.ContentRetriever;
import dev.langchain4j.rag.content.retriever.EmbeddingStoreContentRetriever;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.EmbeddingStoreIngestor;
import dev.langchain4j.store.memory.chat.ChatMemoryStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class Aiconfig {
    @Autowired
    private EmbeddingModel embeddingModel;
    @Autowired
    private ChatMemoryStore redisChatMemoryStore;
    @Autowired
    private RedisEmbeddingStore redisEmbeddingStore;
    //创建一个ChatMemoryProvider,用来创建ChatMemory,做对话记忆持久化
    @Bean
    public ChatMemoryProvider chatMemoryProvider(){
        ChatMemoryProvider chatMemoryProvider = new ChatMemoryProvider() {
            @Override
            public ChatMemory get(Object memoryId) {
                return MessageWindowChatMemory.builder()
                        .id(memoryId)
                        .maxMessages(20)
                        .chatMemoryStore(redisChatMemoryStore)//配置ChatMemoryStore
                        .build();
            }
        };
        return chatMemoryProvider;
    }
    @Bean
//    在第一次运行时加载一次，已经将知识库存储在redis中，第二次运行时把这个bean注释掉，避免重复加载，
    public EmbeddingStore store(){//embeddingStore的对象, 这个对象的名字不能重复,所以这里使用store
        //1.加载文档进内存，相对路径用classpath:，绝对路径用file:
        List<Document> documents = ClassPathDocumentLoader.loadDocuments("content");
        //List<Document> documents = FileSystemDocumentLoader.loadDocuments("C:\\Users\\Administrator\\ideaProjects\\consultant\\src\\main\\resources\\content");
        //2.构建向量数据库操作对象  操作的是内存版本的向量数据库，如果没有配置向量模型，可以将下一行打开，用默认的向量模型
        //InMemoryEmbeddingStore store = new InMemoryEmbeddingStore();

        //构建文档分割器对象
        DocumentSplitter ds = DocumentSplitters.recursive(500,100);
        //3.构建一个EmbeddingStoreIngestor对象,完成文本数据切割,向量化, 存储
        EmbeddingStoreIngestor ingestor = EmbeddingStoreIngestor.builder()
                //.embeddingStore(store)
                .embeddingStore(redisEmbeddingStore)
                .documentSplitter(ds)
                .embeddingModel(embeddingModel)
                .build();
        ingestor.ingest(documents);
        return redisEmbeddingStore;
    }

    @Bean
    public ContentRetriever contentRetriever(/*EmbeddingStore store*/){
        return EmbeddingStoreContentRetriever.builder()
                .embeddingStore(redisEmbeddingStore)
                .minScore(0.5)
                .maxResults(3)
                .embeddingModel(embeddingModel)
                .build();
    }
}