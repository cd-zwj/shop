package com.payment.rag.Config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import java.util.concurrent.Executor;

import static org.junit.jupiter.api.Assertions.assertNotNull;

@DisplayName("AsyncConfig")
class AsyncConfigTest {

    @Test
    @DisplayName("注册 RAG 所需的命名执行器")
    void shouldRegisterNamedExecutors() {
        try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(AsyncConfig.class)) {
            Executor asyncExecutor = context.getBean("asyncTaskExecutor", Executor.class);
            Executor mvcExecutor = context.getBean("mvcTaskExecutor", Executor.class);

            assertNotNull(asyncExecutor);
            assertNotNull(mvcExecutor);
        }
    }
}