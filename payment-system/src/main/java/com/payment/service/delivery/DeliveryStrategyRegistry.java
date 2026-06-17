package com.payment.service.delivery;

import com.payment.enums.ProductTypeEnum;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * 交付策略注册表。
 *
 * 容器启动时把所有 {@link DeliveryStrategy} 实现按 {@link ProductTypeEnum} 收集起来，
 * 调用方直接 {@code registry.get(productType)} 取对应策略。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DeliveryStrategyRegistry {

    private final List<DeliveryStrategy> strategies;
    private final Map<ProductTypeEnum, DeliveryStrategy> registry = new EnumMap<>(ProductTypeEnum.class);

    @PostConstruct
    public void init() {
        for (DeliveryStrategy strategy : strategies) {
            ProductTypeEnum type = strategy.supports();
            DeliveryStrategy existing = registry.put(type, strategy);
            if (existing != null) {
                throw new IllegalStateException("Duplicate DeliveryStrategy for type=" + type
                        + ", existing=" + existing.getClass().getSimpleName()
                        + ", new=" + strategy.getClass().getSimpleName());
            }
        }
        log.info("DeliveryStrategyRegistry initialized with {} strategies: {}", registry.size(), registry.keySet());
    }

    /**
     * 按商品类型获取策略；找不到时返回 null，由调用方决定降级（通常按 PHYSICAL 兜底）。
     */
    public DeliveryStrategy get(ProductTypeEnum type) {
        return registry.get(type);
    }

    public DeliveryStrategy getOrDefault(ProductTypeEnum type) {
        DeliveryStrategy strategy = registry.get(type);
        if (strategy == null) {
            log.warn("No DeliveryStrategy for type={}, fallback to PHYSICAL", type);
            return registry.get(ProductTypeEnum.PHYSICAL);
        }
        return strategy;
    }
}
