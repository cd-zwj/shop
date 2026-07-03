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
 * 商品索引异步消费者
 * <p>
 * 从 {@link RabbitMQConfig#PRODUCT_INDEX_QUEUE} 队列中消费商品变更事件，
 * 将商品数据同步到 Elasticsearch 索引。支持新增/更新和删除两种操作。
 * 当 Elasticsearch 未启用时自动跳过。
 * </p>
 *
 * @author payment-system
 */
@Slf4j
@Component
public class ProductIndexConsumer {

    /**
     * Elasticsearch 商品仓储（可选注入，ES 未启用时为 null）
     */
    @Autowired(required = false)
    private ProductRepository productRepository;

    /**
     * 处理商品索引消息
     * <p>
     * 根据消息中的操作类型执行对应操作：
     * <ul>
     *   <li>删除操作或已标记删除 —— 从 ES 中删除对应文档</li>
     *   <li>其他操作 —— 将商品信息保存到 ES 索引</li>
     * </ul>
     * </p>
     *
     * @param body 消息体 JSON 字符串，反序列化为 {@link ProductIndexMessage}
     */
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
            throw e;
        } finally {
            TenantContextHolder.clear();
        }
    }
}
