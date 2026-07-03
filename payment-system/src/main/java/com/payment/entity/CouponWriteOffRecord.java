package com.payment.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 优惠券核销记录实体，对应数据库表 coupon_write_off_record。
 * <p>
 * 订单支付成功后对已锁定的优惠券进行核销，记录实际优惠金额。
 * 一条核销记录对应一次优惠券的使用，同时更新模板的已使用数量。
 * </p>
 */
@Data
@TableName("coupon_write_off_record")
public class CouponWriteOffRecord implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 主键ID，自增 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 关联的用户优惠券ID，对应 user_coupon 表 */
    private Long userCouponId;

    /** 关联的优惠券模板ID，对应 coupon_template 表 */
    private Long couponTemplateId;

    /** 所属租户ID，多租户行级隔离 */
    private Long tenantId;

    /** 关联的订单ID */
    private Long orderId;

    /** 关联的订单编号 */
    private String orderNo;

    /** 业务流水号，用于幂等校验和链路追踪 */
    private String bizNo;

    /** 实际核销的优惠金额，单位：元 */
    private BigDecimal discountAmount;

    /** 核销时间 */
    private LocalDateTime writeOffTime;
}
