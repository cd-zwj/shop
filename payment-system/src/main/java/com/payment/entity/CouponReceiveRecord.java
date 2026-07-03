package com.payment.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 优惠券领取记录实体，对应数据库表 coupon_receive_record。
 * <p>
 * 记录用户领取优惠券的流水，每次领取生成一条记录，关联用户持有的优惠券（user_coupon）
 * 和来源模板（coupon_template），用于领取统计和审计追踪。
 * </p>
 */
@Data
@TableName("coupon_receive_record")
public class CouponReceiveRecord implements Serializable {
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

    /** 领取用户ID，对应 platform_user 表 */
    private Long platformUserId;

    /** 业务流水号，用于幂等校验和链路追踪 */
    private String bizNo;

    /** 领取时间 */
    private LocalDateTime receiveTime;
}
