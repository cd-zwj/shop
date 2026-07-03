package com.payment.service;

import com.payment.dto.ActivityRuleCreateDTO;
import com.payment.dto.PromotionActivityCreateDTO;
import com.payment.dto.pricing.OrderPricingItemDTO;
import com.payment.dto.pricing.PromotionDiscountCandidateDTO;
import com.payment.entity.ActivityRule;
import com.payment.entity.PromotionActivity;

import java.util.List;

/**
 * 营销活动服务接口。
 *
 * <p>提供营销活动（如满减、折扣、赠品等）的管理能力，包括活动创建、规则配置、
 * 启停控制以及订单定价时的活动匹配。支持商户级和平台级两种维度的营销活动。</p>
 */
public interface PromotionService {

    /**
     * 查询商户营销活动列表。
     *
     * @param tenantId 租户ID
     * @param status   活动状态过滤：draft / active / disabled（可空）
     * @return 营销活动列表
     */
    List<PromotionActivity> listActivities(Long tenantId, String status);

    /**
     * 查询平台级营销活动列表。
     *
     * @param status 活动状态过滤（可空）
     * @return 平台营销活动列表
     */
    List<PromotionActivity> listPlatformActivities(String status);

    /**
     * 查询指定活动的规则列表（商户级）。
     *
     * @param activityId 营销活动ID
     * @param tenantId   租户ID
     * @return 活动规则列表
     */
    List<ActivityRule> listRules(Long activityId, Long tenantId);

    /**
     * 查询指定平台活动的规则列表。
     *
     * @param activityId 营销活动ID
     * @return 活动规则列表
     */
    List<ActivityRule> listPlatformRules(Long activityId);

    /**
     * 创建营销活动，默认为草稿状态。
     *
     * @param dto 创建参数，包含活动名称、类型、时间范围等
     * @return 创建成功的营销活动实体
     * @throws com.payment.common.exception.BusinessException 当参数校验失败时抛出
     */
    PromotionActivity createActivity(PromotionActivityCreateDTO dto);

    /**
     * 新增营销活动规则（如满减门槛、折扣比例等）。
     *
     * @param dto 规则参数，关联活动ID和规则条件
     * @return 创建成功的规则实体
     */
    ActivityRule addRule(ActivityRuleCreateDTO dto);

    /**
     * 启用营销活动。
     *
     * @param activityId 营销活动ID
     * @throws com.payment.common.exception.BusinessException 当活动状态非草稿时抛出
     */
    void activateActivity(Long activityId);

    /**
     * 停用营销活动。
     *
     * @param activityId 营销活动ID
     */
    void disableActivity(Long activityId);

    /**
     * 根据订单商品列表匹配可用的营销活动折扣候选项。
     *
     * <p>在订单定价流程中调用，遍历当前有效的营销活动规则，
     * 返回所有匹配的折扣候选及其可优惠金额，由定价引擎选择最优方案。</p>
     *
     * @param tenantId 租户ID
     * @param items    订单商品明细列表
     * @return 匹配到的营销活动折扣候选项列表
     */
    List<PromotionDiscountCandidateDTO> matchPromotions(Long tenantId, List<OrderPricingItemDTO> items);
}
