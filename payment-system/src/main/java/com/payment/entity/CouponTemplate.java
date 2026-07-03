package com.payment.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 优惠券模板实体，对应数据库表 coupon_template。
 * <p>
 * 定义优惠券的基本规则，包括类型（满减/折扣/直减）、面额、领取限制、
 * 有效期规则、叠加策略、适用商品范围等。一个模板可生成多张用户优惠券。
 * </p>
 */
@Data
@TableName("coupon_template")
public class CouponTemplate implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 主键ID，自增 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 模板编号，业务唯一标识 */
    private String templateNo;

    /** 所属租户ID，多租户行级隔离 */
    private Long tenantId;

    /** 模板作用域，如 PLATFORM-平台券 / MERCHANT-商户券 */
    private String templateScope;

    /** 模板名称，用于前端展示 */
    private String templateName;

    /**
     * 优惠券类型。
     * THRESHOLD_REDUCE-满减券、DISCOUNT-折扣券、DIRECT_REDUCE-直减券、FREE-免费券
     */
    private String couponType;

    /** 满减门槛金额，满减券需要达到此金额方可使用，单位：元 */
    private BigDecimal thresholdAmount;

    /** 优惠金额，满减券/直减券的具体减免金额，单位：元 */
    private BigDecimal discountAmount;

    /** 折扣率，折扣券使用，如 0.85 表示八五折 */
    private BigDecimal discountRate;

    /** 折扣券最大优惠金额上限，防止高额订单优惠过多，单位：元 */
    private BigDecimal maxDiscountAmount;

    /** 发放总数量，-1 表示不限量 */
    private Integer totalQuantity;

    /** 已领取数量，冗余字段，用于快速判断库存 */
    private Integer receivedQuantity;

    /** 已使用（核销）数量 */
    private Integer usedQuantity;

    /** 每位用户限领数量，防止刷券 */
    private Integer perUserLimit;

    /** 领取开始时间，在此时间之前不可领取 */
    private LocalDateTime receiveStartTime;

    /** 领取结束时间，在此时间之后不可领取 */
    private LocalDateTime receiveEndTime;

    /**
     * 有效期类型。
     * FIXED-固定时间段、FIXED_DATE-领取后N天有效（见 validDays）
     */
    private String validType;

    /** 领取后有效天数，validType 为 FIXED_DATE 时生效 */
    private Integer validDays;

    /** 固定有效期开始时间，validType 为 FIXED 时生效 */
    private LocalDateTime validStartTime;

    /** 固定有效期结束时间，validType 为 FIXED 时生效 */
    private LocalDateTime validEndTime;

    /** 是否可与钱包余额叠加使用 */
    private Boolean canStackBalance;

    /** 是否可与积分叠加使用 */
    private Boolean canStackPoints;

    /** 是否可与其他优惠券叠加使用 */
    private Boolean canStackOtherCoupon;

    /**
     * 适用商品范围类型。
     * ALL-全场通用、PRODUCT-指定商品、CATEGORY-指定分类
     */
    private String applicableProductScope;

    /** 适用商品/分类的具体配置，JSON 数组格式存储 ID 列表 */
    private String applicableProductJson;

    /** 优惠券描述说明，用于前端展示 */
    private String description;

    /**
     * 模板状态。
     * DRAFT-草稿、ACTIVE-上架生效中、PAUSED-暂停发放、FINISHED-已领完、CLOSED-已关闭
     */
    private String status;

    /** 逻辑删除标记，0-未删除、1-已删除 */
    private Integer deleted;

    /** 创建时间 */
    private LocalDateTime createTime;

    /** 最后更新时间 */
    private LocalDateTime updateTime;
}
