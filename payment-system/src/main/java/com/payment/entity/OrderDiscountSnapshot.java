package com.payment.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 订单优惠快照实体，仅冻结活动与优惠券折扣。
 */
@Data
@TableName("order_discount_snapshot")
public class OrderDiscountSnapshot implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long orderId;
    private String orderNo;
    private Long tenantId;
    private Long activityId;
    private Long activityRuleId;
    private Long userCouponId;
    private Long couponTemplateId;
    private String discountSource;
    private String discountType;
    private BigDecimal discountAmount;
    private String ruleSnapshotJson;
    private LocalDateTime createTime;
}
