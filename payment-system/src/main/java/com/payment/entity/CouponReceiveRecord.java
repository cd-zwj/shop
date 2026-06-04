package com.payment.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 优惠券领取记录实体。
 */
@Data
@TableName("coupon_receive_record")
public class CouponReceiveRecord implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userCouponId;
    private Long couponTemplateId;
    private Long tenantId;
    private Long platformUserId;
    private String bizNo;
    private LocalDateTime receiveTime;
}
