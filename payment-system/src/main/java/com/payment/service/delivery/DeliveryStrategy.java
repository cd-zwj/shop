package com.payment.service.delivery;

import com.payment.entity.OrderDeliveryRecord;
import com.payment.entity.Product;
import com.payment.entity.SalesOrder;
import com.payment.entity.SalesOrderItem;
import com.payment.enums.ProductTypeEnum;

/**
 * 商品交付策略。
 *
 * 每种商品类型对应一个实现，{@link DeliveryStrategyRegistry} 在容器启动时自动收集。
 */
public interface DeliveryStrategy {

    /** 本策略对应的商品类型。 */
    ProductTypeEnum supports();

    /**
     * 执行交付。
     *
     * 入参里同时给到 order / item / product，避免实现类各自再查库。
     * 实现方负责生成 payload（卡密 / URL / 核销码 / 物流单号等），但不负责落库 —— 落库由 OrderDeliveryService 统一做。
     */
    DeliveryResult deliver(SalesOrder order, SalesOrderItem item, Product product);

    /**
     * 退款回收。第一版各 strategy 用默认空实现，框架接口先留好，
     * 后续按品类补真实回收逻辑（卡密作废、权益冻结、物流拦截等）。
     */
    default void revoke(OrderDeliveryRecord record) {
        // 默认空实现
    }
}
