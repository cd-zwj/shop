package com.payment.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 营销活动实体，对应数据库表 marketing_activity。
 * <p>记录商户创建的各类营销活动信息，包括满减、折扣、赠券等活动形式，
 * 支持按活动时间、状态进行生命周期管理。</p>
 */
@Data
@TableName("marketing_activity")
public class PromotionActivity implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 主键ID，自增 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 活动编号，全局唯一，用于业务查询和对外展示 */
    private String activityNo;

    /** 所属租户ID */
    private Long tenantId;

    /** 活动适用范围：ALL-全场通用，CATEGORY-指定分类，PRODUCT-指定商品等 */
    private String activityScope;

    /** 活动名称，用于前端展示 */
    private String activityName;

    /** 活动类型：FULL_REDUCTION-满减，DISCOUNT-折扣，GIVE_COUPON-赠券，FLASH_SALE-秒杀等 */
    private String activityType;

    /** 活动开始时间 */
    private LocalDateTime startTime;

    /** 活动结束时间 */
    private LocalDateTime endTime;

    /** 活动状态：DRAFT-草稿，ACTIVE-进行中，PAUSED-已暂停，ENDED-已结束 */
    private String status;

    /** 活动规则配置，JSON 格式，包含门槛金额、折扣比例等具体规则参数 */
    private String ruleJson;

    /** 关联的优惠券模板ID，赠券类活动时使用，非赠券活动为 null */
    private Long grantCouponTemplateId;

    /** 活动详细描述，支持富文本 */
    private String description;

    /** 软删除标记：0-正常，1-已删除 */
    private Integer deleted;

    /** 活动创建时间 */
    private LocalDateTime createTime;

    /** 记录最后更新时间 */
    private LocalDateTime updateTime;
}
