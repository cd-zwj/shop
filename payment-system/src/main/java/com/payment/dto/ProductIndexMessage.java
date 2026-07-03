package com.payment.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.payment.entity.Product;
import lombok.Data;
import org.springframework.beans.BeanUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 商品索引异步消息，通过 Outbox + RabbitMQ 投递到 ES 索引消费者。
 */
@Data
public class ProductIndexMessage {

    /** 操作类型：新增/更新 */
    public static final String ACTION_UPSERT = "UPSERT";

    /** 操作类型：删除 */
    public static final String ACTION_DELETE = "DELETE";

    /** 操作类型（UPSERT / DELETE） */
    private String action;

    /** 商品 ID */
    private Long id;

    /** 所属租户 ID */
    private Long tenantId;

    /** 商品编码 */
    private String productCode;

    /** 商品名称 */
    private String name;

    /** 商品单价 */
    private BigDecimal price;

    /** 计量单位 */
    private String unit;

    /** 商品分类 */
    private String category;

    /** 商品图片 URL */
    private String imageUrl;

    /** 商品描述 */
    private String description;

    /** 商品状态 */
    private Integer status;

    /** 逻辑删除标记（0-未删除，1-已删除） */
    private Integer deleted;

    /** 创建时间 */
    private LocalDateTime createTime;

    /** 最后更新时间 */
    private LocalDateTime updateTime;

    /**
     * 从商品实体构建 UPSERT 索引消息。
     *
     * @param product 商品实体
     * @return 索引消息
     */
    public static ProductIndexMessage upsert(Product product) {
        return fromProduct(ACTION_UPSERT, product);
    }

    /**
     * 从商品实体构建 DELETE 索引消息。
     *
     * @param product 商品实体
     * @return 索引消息
     */
    public static ProductIndexMessage delete(Product product) {
        return fromProduct(ACTION_DELETE, product);
    }

    private static ProductIndexMessage fromProduct(String action, Product product) {
        ProductIndexMessage message = new ProductIndexMessage();
        BeanUtils.copyProperties(product, message);
        message.setAction(action);
        return message;
    }

    /**
     * 判断是否为删除操作。
     *
     * @return true 表示删除操作
     */
    @JsonIgnore
    public boolean isDeleteAction() {
        return ACTION_DELETE.equalsIgnoreCase(action);
    }
}
