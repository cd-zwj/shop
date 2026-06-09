package com.payment.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 优惠券模板实体。
 */
@Data
@TableName("coupon_template")
public class CouponTemplate implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;
    private String templateNo;
    private Long tenantId;
    private String templateScope;
    private String templateName;
    private String couponType;
    private BigDecimal thresholdAmount;
    private BigDecimal discountAmount;
    private BigDecimal discountRate;
    private BigDecimal maxDiscountAmount;
    private Integer totalQuantity;
    private Integer receivedQuantity;
    private Integer usedQuantity;
    private Integer perUserLimit;
    private LocalDateTime receiveStartTime;
    private LocalDateTime receiveEndTime;
    private String validType;
    private Integer validDays;
    private LocalDateTime validStartTime;
    private LocalDateTime validEndTime;
    private Boolean canStackBalance;
    private Boolean canStackPoints;
    private Boolean canStackOtherCoupon;
    private String applicableProductScope;
    private String applicableProductJson;
    private String description;
    private String status;
    private Integer deleted;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
