package com.payment.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.payment.entity.Product;
import lombok.Data;
import org.springframework.beans.BeanUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 商品索引异步消息。
 */
@Data
public class ProductIndexMessage {

    public static final String ACTION_UPSERT = "UPSERT";
    public static final String ACTION_DELETE = "DELETE";

    private String action;
    private Long id;
    private Long tenantId;
    private String productCode;
    private String name;
    private BigDecimal price;
    private String unit;
    private String category;
    private String imageUrl;
    private String description;
    private Integer status;
    private Integer deleted;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;

    public static ProductIndexMessage upsert(Product product) {
        return fromProduct(ACTION_UPSERT, product);
    }

    public static ProductIndexMessage delete(Product product) {
        return fromProduct(ACTION_DELETE, product);
    }

    private static ProductIndexMessage fromProduct(String action, Product product) {
        ProductIndexMessage message = new ProductIndexMessage();
        BeanUtils.copyProperties(product, message);
        message.setAction(action);
        return message;
    }

    @JsonIgnore
    public boolean isDeleteAction() {
        return ACTION_DELETE.equalsIgnoreCase(action);
    }
}
