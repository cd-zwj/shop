package com.payment.service.delivery.impl;

import com.payment.entity.Product;
import com.payment.entity.SalesOrder;
import com.payment.entity.SalesOrderItem;
import com.payment.enums.ProductTypeEnum;
import com.payment.service.delivery.DeliveryResult;
import com.payment.service.delivery.DeliveryStrategy;
import org.springframework.stereotype.Component;

/**
 * 实物交付策略。
 *
 * 支付成功后落 PENDING，等待商户在后台点"发货"填物流单号 ——
 * 后续流转 (DELIVERED 单号回填、CONFIRMED 用户确认) 由 OrderDeliveryService 提供专用方法处理。
 */
@Component
public class PhysicalDeliveryStrategy implements DeliveryStrategy {

    @Override
    public ProductTypeEnum supports() {
        return ProductTypeEnum.PHYSICAL;
    }

    @Override
    public DeliveryResult deliver(SalesOrder order, SalesOrderItem item, Product product) {
        // 实物先入 PENDING，payload 暂时为空，商户填单号时再写入
        return DeliveryResult.pending(null);
    }
}
