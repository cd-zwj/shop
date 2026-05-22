package com.payment.consumer;

import com.payment.document.ProductDocument;
import com.payment.dto.ProductIndexMessage;
import com.payment.repository.ProductRepository;
import com.payment.util.JsonUtils;
import com.payment.util.TenantContextHolder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class ProductIndexConsumerTest {

    @AfterEach
    void tearDown() {
        TenantContextHolder.clear();
    }

    @Test
    void handleProductIndexShouldSaveDocumentForUpsert() {
        ProductRepository productRepository = mock(ProductRepository.class);
        ProductIndexConsumer consumer = new ProductIndexConsumer();
        ReflectionTestUtils.setField(consumer, "productRepository", productRepository);

        ProductIndexMessage message = new ProductIndexMessage();
        message.setAction(ProductIndexMessage.ACTION_UPSERT);
        message.setId(11L);
        message.setTenantId(22L);
        message.setName("雪碧");
        message.setStatus(1);
        message.setDeleted(0);

        consumer.handleProductIndex(JsonUtils.toJson(message));

        verify(productRepository).save(org.mockito.ArgumentMatchers.argThat(document -> {
            ProductDocument productDocument = (ProductDocument) document;
            return productDocument.getId().equals(11L)
                    && productDocument.getTenantId().equals(22L)
                    && "雪碧".equals(productDocument.getName());
        }));
        assertNull(TenantContextHolder.getTenantId());
    }

    @Test
    void handleProductIndexShouldDeleteDocumentForDeleteAction() {
        ProductRepository productRepository = mock(ProductRepository.class);
        ProductIndexConsumer consumer = new ProductIndexConsumer();
        ReflectionTestUtils.setField(consumer, "productRepository", productRepository);

        ProductIndexMessage message = new ProductIndexMessage();
        message.setAction(ProductIndexMessage.ACTION_DELETE);
        message.setId(33L);
        message.setTenantId(44L);

        consumer.handleProductIndex(JsonUtils.toJson(message));

        verify(productRepository).deleteById(33L);
        assertNull(TenantContextHolder.getTenantId());
    }

    @Test
    void handleProductIndexShouldSkipWhenRepositoryDisabled() {
        ProductIndexConsumer consumer = new ProductIndexConsumer();
        ProductIndexMessage message = new ProductIndexMessage();
        message.setAction(ProductIndexMessage.ACTION_UPSERT);
        message.setId(55L);
        message.setTenantId(66L);

        consumer.handleProductIndex(JsonUtils.toJson(message));

        assertNull(TenantContextHolder.getTenantId());
    }
}
