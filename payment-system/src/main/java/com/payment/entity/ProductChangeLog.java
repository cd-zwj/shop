package com.payment.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 商品价格/库存变更流水。
 */
@Data
@TableName("product_change_log")
public class ProductChangeLog implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long tenantId;

    private Long productId;

    /** PRICE / STOCK */
    private String changeType;

    /** price / stock */
    private String fieldName;

    private String oldValue;

    private String newValue;

    private Long operatorId;

    private String remark;

    private LocalDateTime createTime;
}
