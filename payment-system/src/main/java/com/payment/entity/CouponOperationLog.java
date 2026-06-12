package com.payment.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 优惠券操作日志。
 */
@Data
@TableName("coupon_operation_log")
public class CouponOperationLog implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;
    private String couponNo;
    private Long platformUserId;
    private Long tenantId;
    /** 操作类型：RECEIVE/LOCK/UNLOCK/USE/EXPIRE/VOID */
    private String operationType;
    private String beforeStatus;
    private String afterStatus;
    /** 业务类型：SALES_ORDER/RECHARGE_ORDER/ACTIVITY */
    private String bizType;
    private String bizNo;
    private String remark;
    private LocalDateTime createTime;
}
