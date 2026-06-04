package com.payment.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 优惠券锁定记录实体。
 */
@Data
@TableName("coupon_lock_record")
public class CouponLockRecord implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userCouponId;
    private Long tenantId;
    private Long orderId;
    private String orderNo;
    private String bizNo;
    private LocalDateTime lockTime;
    private String lockStatus;
}
