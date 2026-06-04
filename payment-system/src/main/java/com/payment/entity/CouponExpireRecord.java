package com.payment.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 优惠券过期记录实体。
 */
@Data
@TableName("coupon_expire_record")
public class CouponExpireRecord implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userCouponId;
    private Long couponTemplateId;
    private Long tenantId;
    private Long platformUserId;
    private String bizNo;
    private String expireReason;
    private LocalDateTime expireTime;
}
