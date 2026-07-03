package com.payment.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 优惠券过期记录实体，对应数据库表 coupon_expire_record。
 * <p>
 * 记录用户持有的优惠券因超过有效期或模板关闭等原因而过期的流水，
 * 通常由定时任务批量扫描生成，用于过期统计和用户通知。
 * </p>
 */
@Data
@TableName("coupon_expire_record")
public class CouponExpireRecord implements Serializable {
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

    /** 持有该优惠券的用户ID，对应 platform_user 表 */
    private Long platformUserId;

    /** 业务流水号，用于幂等校验和链路追踪 */
    private String bizNo;

    /**
     * 过期原因。
     * TIME_EXPIRED-超过有效期、TEMPLATE_CLOSED-模板关闭、MANUAL-管理员手动作废
     */
    private String expireReason;

    /** 过期时间 */
    private LocalDateTime expireTime;
}
