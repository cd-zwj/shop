package com.payment.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.Version;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 用户优惠券实体。
 */
@Data
@TableName("user_coupon")
public class UserCoupon implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;
    private String couponNo;
    private Long templateId;
    private Long tenantId;
    private Long platformUserId;
    private String sourceType;
    private String sourceBizNo;
    private String couponStatus;
    private String orderNo;
    private LocalDateTime lockTime;
    private LocalDateTime useTime;
    private LocalDateTime expireTime;
    @Version
    private Integer version;
    private LocalDateTime receiveTime;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
