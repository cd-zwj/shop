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
 */
public interface PromotionService {

    /**
     * 查询商户营销活动。
     */
    List<PromotionActivity> listActivities(Long tenantId, String status);

    /**
     * 查询平台营销活动。
     */
    List<PromotionActivity> listPlatformActivities(String status);

    /**
     * 查询活动规则。
     */
    List<ActivityRule> listRules(Long activityId, Long tenantId);

    /**
     * 查询平台活动规则。
     */
    List<ActivityRule> listPlatformRules(Long activityId);

    /**
     * 创建营销活动，默认草稿状态。
     */
    PromotionActivity createActivity(PromotionActivityCreateDTO dto);

    /**
     * 新增营销活动规则。
     */
    ActivityRule addRule(ActivityRuleCreateDTO dto);

    /**
     * 启用营销活动。
     */
    void activateActivity(Long activityId);

    /**
     * 停用营销活动。
     */
    void disableActivity(Long activityId);

    /**
     * 根据订单商品匹配可用活动折扣候选。
     */
    List<PromotionDiscountCandidateDTO> matchPromotions(Long tenantId, List<OrderPricingItemDTO> items);
}
