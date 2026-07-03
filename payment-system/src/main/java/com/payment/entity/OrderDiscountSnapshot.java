package com.payment.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 订单优惠快照实体，对应数据库表 order_discount_snapshot。
 * <p>
 * 下单时冻结活动与优惠券的折扣信息，防止活动结束后无法追溯订单实际享受的优惠。
 * 每条记录对应一次优惠来源（活动规则或用户优惠券），一张订单可有多条快照。
 * </p>
 */
@Data
@TableName("order_discount_snapshot")
public class OrderDiscountSnapshot implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 主键 ID，自增 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 所属订单 ID，关联 sales_order.id */
    private Long orderId;

    /** 所属订单编号，冗余字段便于查询 */
    private String orderNo;

    /** 租户 ID，多租户行级隔离字段 */
    private Long tenantId;

    /** 营销活动 ID，关联营销活动表，来自活动折扣时有值 */
    private Long activityId;

    /** 活动规则 ID，关联活动规则明细表，来自活动折扣时有值 */
    private Long activityRuleId;

    /** 用户优惠券 ID，关联 user_coupon 表，来自优惠券抵扣时有值 */
    private Long userCouponId;

    /** 优惠券模板 ID，关联 coupon_template 表，来自优惠券抵扣时有值 */
    private Long couponTemplateId;

    /**
     * 优惠来源类型，取值如：ACTIVITY(营销活动) / COUPON(优惠券)
     */
    private String discountSource;

    /**
     * 优惠类型，取值如：FIXED_AMOUNT(满减) / PERCENT(折扣) / FREE_SHIPPING(免邮) / GIFT(赠品)
     */
    private String discountType;

    /** 优惠减免金额，单位：元 */
    private BigDecimal discountAmount;

    /** 活动/优惠券规则快照 JSON，冻结下单时的规则详情，防止规则变更后无法追溯 */
    private String ruleSnapshotJson;

    /** 记录创建时间 */
    private LocalDateTime createTime;
}
