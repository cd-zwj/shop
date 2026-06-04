package com.payment.service;

import com.payment.entity.UserCoupon;
import com.payment.dto.AppCouponReceiveVO;
import com.payment.dto.AppCouponTemplateVO;
import com.payment.dto.AppUserCouponVO;
import com.payment.dto.CouponScopeCreateDTO;
import com.payment.dto.CouponTemplateCreateDTO;
import com.payment.dto.pricing.CouponDiscountCandidateDTO;
import com.payment.dto.pricing.OrderPricingItemDTO;
import com.payment.entity.CouponScope;
import com.payment.entity.CouponTemplate;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 优惠券服务接口。
 */
public interface CouponService {
    /**
     * 查询商户优惠券模板。
     */
    List<CouponTemplate> listTemplates(Long tenantId, String status);

    /**
     * 查询平台优惠券模板。
     */
    List<CouponTemplate> listPlatformTemplates(String status);

    /**
     * 查询优惠券模板适用范围。
     */
    List<CouponScope> listScopes(Long couponTemplateId, Long tenantId);

    /**
     * 查询平台优惠券模板适用范围。
     */
    List<CouponScope> listPlatformScopes(Long couponTemplateId);

    /**
     * 创建优惠券模板，默认草稿状态。
     */
    CouponTemplate createTemplate(CouponTemplateCreateDTO dto);

    /**
     * 新增优惠券适用范围。
     */
    CouponScope addScope(CouponScopeCreateDTO dto);

    /**
     * 启用优惠券模板。
     */
    void activateTemplate(Long couponTemplateId);

    /**
     * 停用优惠券模板。
     */
    void disableTemplate(Long couponTemplateId);

    /**
     * 查询用户端可领取优惠券模板。
     */
    List<AppCouponTemplateVO> listAvailableTemplates(Long tenantId, Long platformUserId);

    /**
     * 查询用户券包。
     */
    List<AppUserCouponVO> listUserCoupons(Long tenantId, Long platformUserId, String status);

    /**
     * 领取优惠券。
     */
    UserCoupon receiveCoupon(Long couponTemplateId, Long tenantId, Long platformUserId, String bizNo);

    /**
     * 领取优惠券并返回用户端视图。
     */
    AppCouponReceiveVO receiveCouponForApp(Long couponTemplateId, Long tenantId, Long platformUserId);

    /**
     * 解析用户券为订单定价候选项，并校验用户、租户、范围和状态。
     */
    CouponDiscountCandidateDTO resolveCouponCandidate(Long userCouponId,
                                                      Long tenantId,
                                                      Long platformUserId,
                                                      List<OrderPricingItemDTO> items);

    /**
     * 锁定用户券。
     */
    void lockCoupon(Long userCouponId, Long tenantId, Long platformUserId, Long orderId, String orderNo, String bizNo);

    /**
     * 释放用户券。
     */
    void releaseCoupon(Long userCouponId, Long tenantId, Long platformUserId, Long orderId, String orderNo, String bizNo, String releaseReason);

    /**
     * 核销用户券。
     */
    void writeOffCoupon(Long userCouponId, Long tenantId, Long orderId, String orderNo, String bizNo, BigDecimal discountAmount);

    /**
     * 扫描并过期已到期的用户券。
     */
    int expireCoupons(Long tenantId, LocalDateTime expireBefore, String bizNo, String expireReason);
}
