package com.payment.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 优惠券释放记录实体。
 */
@Data
@TableName("coupon_release_record")
public class CouponReleaseRecord implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userCouponId;
    private Long tenantId;
    private Long orderId;
    private String orderNo;
    private String bizNo;
    private String releaseReason;
    private LocalDateTime releaseTime;
}
