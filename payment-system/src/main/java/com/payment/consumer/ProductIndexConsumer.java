package com.payment.consumer;

import com.payment.config.RabbitMQConfig;
import com.payment.document.ProductDocument;
import com.payment.dto.ProductIndexMessage;
import com.payment.repository.ProductRepository;
import com.payment.util.JsonUtils;
import com.payment.util.TenantContextHolder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

/**
 * 商品索引异步消费者。
 */
@Slf4j
@Component
public class ProductIndexConsumer {

    @Autowired(required = false)
    private ProductRepository productRepository;

    @RabbitListener(queues = RabbitMQConfig.PRODUCT_INDEX_QUEUE)
    public void handleProductIndex(String body) {
        ProductIndexMessage message = JsonUtils.fromJson(body, ProductIndexMessage.class);
        if (productRepository == null) {
            log.warn("商品索引消息已跳过，Elasticsearch repository 未启用，tenantId={}, productId={}",
                    message.getTenantId(), message.getId());
            return;
        }

        try {
            TenantContextHolder.setTenantId(message.getTenantId());
            if (message.isDeleteAction() || Integer.valueOf(1).equals(message.getDeleted())) {
                productRepository.deleteById(message.getId());
                log.info("删除商品索引成功，tenantId={}, productId={}", message.getTenantId(), message.getId());
                return;
            }

            ProductDocument document = new ProductDocument();
            BeanUtils.copyProperties(message, document);
            productRepository.save(document);
            log.info("同步商品索引成功，tenantId={}, productId={}", message.getTenantId(), message.getId());
        } catch (Exception e) {
            log.error("处理商品索引消息失败，tenantId={}, productId={}", message.getTenantId(), message.getId(), e);
        } finally {
            TenantContextHolder.clear();
        }
    }
}
