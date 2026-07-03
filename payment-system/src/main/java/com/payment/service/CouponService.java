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
 *
 * <p>提供优惠券全生命周期管理能力，涵盖模板管理（创建、启用、停用）、
 * 适用范围配置、用户领券、锁券、释放、核销以及过期处理等。
 * 支持商户级优惠券和平台级优惠券两种维度。</p>
 */
public interface CouponService {
    /**
     * 查询商户优惠券模板列表。
     *
     * @param tenantId 租户ID
     * @param status   模板状态过滤：draft / active / disabled（可空）
     * @return 优惠券模板列表
     */
    List<CouponTemplate> listTemplates(Long tenantId, String status);

    /**
     * 查询平台级优惠券模板列表。
     *
     * @param status 模板状态过滤（可空）
     * @return 平台优惠券模板列表
     */
    List<CouponTemplate> listPlatformTemplates(String status);

    /**
     * 查询优惠券模板的适用范围（可使用的商品/分类）。
     *
     * @param couponTemplateId 优惠券模板ID
     * @param tenantId         租户ID
     * @return 适用范围列表
     */
    List<CouponScope> listScopes(Long couponTemplateId, Long tenantId);

    /**
     * 查询平台优惠券模板的适用范围。
     *
     * @param couponTemplateId 优惠券模板ID
     * @return 适用范围列表
     */
    List<CouponScope> listPlatformScopes(Long couponTemplateId);

    /**
     * 创建优惠券模板，默认为草稿状态。
     *
     * @param dto 创建参数，包含名称、面额/折扣率、有效期、领取上限等
     * @return 创建成功的优惠券模板实体
     * @throws com.payment.common.exception.BusinessException 当参数校验失败时抛出
     */
    CouponTemplate createTemplate(CouponTemplateCreateDTO dto);

    /**
     * 新增优惠券模板的适用范围。
     *
     * @param dto 范围参数，关联模板ID与商品/分类
     * @return 创建成功的适用范围实体
     */
    CouponScope addScope(CouponScopeCreateDTO dto);

    /**
     * 启用优惠券模板，使其可供用户领取。
     *
     * @param couponTemplateId 优惠券模板ID
     * @throws com.payment.common.exception.BusinessException 当模板状态非草稿或已启用时抛出
     */
    void activateTemplate(Long couponTemplateId);

    /**
     * 停用优惠券模板，停止用户领取。
     *
     * @param couponTemplateId 优惠券模板ID
     */
    void disableTemplate(Long couponTemplateId);

    /**
     * 查询用户端可领取的优惠券模板列表。
     *
     * <p>返回状态为启用中且未超过领取上限的模板。</p>
     *
     * @param tenantId       租户ID
     * @param platformUserId 平台用户ID
     * @return 可领取优惠券模板视图列表
     */
    List<AppCouponTemplateVO> listAvailableTemplates(Long tenantId, Long platformUserId);

    /**
     * 查询用户的券包（已领取的优惠券）。
     *
     * @param tenantId       租户ID
     * @param platformUserId 平台用户ID
     * @param status         券状态过滤：available / locked / used / expired（可空）
     * @return 用户优惠券视图列表
     */
    List<AppUserCouponVO> listUserCoupons(Long tenantId, Long platformUserId, String status);

    /**
     * 用户领取优惠券。
     *
     * @param couponTemplateId 优惠券模板ID
     * @param tenantId         租户ID
     * @param platformUserId   平台用户ID
     * @param bizNo            业务单号（幂等键）
     * @return 领取成功的用户优惠券实体
     * @throws com.payment.common.exception.BusinessException 当超出领取上限、模板已停用或券已领完时抛出
     */
    UserCoupon receiveCoupon(Long couponTemplateId, Long tenantId, Long platformUserId, String bizNo);

    /**
     * 用户领取优惠券并返回App端视图对象。
     *
     * @param couponTemplateId 优惠券模板ID
     * @param tenantId         租户ID
     * @param platformUserId   平台用户ID
     * @return 领取结果视图，包含券信息和提示消息
     * @throws com.payment.common.exception.BusinessException 当领取条件不满足时抛出
     */
    AppCouponReceiveVO receiveCouponForApp(Long couponTemplateId, Long tenantId, Long platformUserId);

    /**
     * 解析用户券为订单定价候选项，校验用户、租户、适用范围和券状态。
     *
     * <p>在订单定价流程中调用，计算该券对当前订单商品的可抵扣金额。</p>
     *
     * @param userCouponId   用户优惠券ID
     * @param tenantId       租户ID
     * @param platformUserId 平台用户ID
     * @param items          订单商品明细列表
     * @return 优惠券折扣候选项，包含可抵扣金额等信息
     * @throws com.payment.common.exception.BusinessException 当券不适用或状态异常时抛出
     */
    CouponDiscountCandidateDTO resolveCouponCandidate(Long userCouponId,
                                                      Long tenantId,
                                                      Long platformUserId,
                                                      List<OrderPricingItemDTO> items);

    /**
     * 锁定用户券（订单创建时调用，防止券被重复使用）。
     *
     * @param userCouponId   用户优惠券ID
     * @param tenantId       租户ID
     * @param platformUserId 平台用户ID
     * @param orderId        订单ID
     * @param orderNo        订单号
     * @param bizNo          业务单号（幂等键）
     * @throws com.payment.common.exception.BusinessException 当券已被锁定或已使用时抛出
     */
    void lockCoupon(Long userCouponId, Long tenantId, Long platformUserId, Long orderId, String orderNo, String bizNo);

    /**
     * 释放已锁定的用户券（订单取消或超时未支付时调用）。
     *
     * @param userCouponId   用户优惠券ID
     * @param tenantId       租户ID
     * @param platformUserId 平台用户ID
     * @param orderId        订单ID
     * @param orderNo        订单号
     * @param bizNo          业务单号（幂等键）
     * @param releaseReason  释放原因
     */
    void releaseCoupon(Long userCouponId, Long tenantId, Long platformUserId, Long orderId, String orderNo, String bizNo, String releaseReason);

    /**
     * 核销用户券（订单支付成功后调用）。
     *
     * @param userCouponId   用户优惠券ID
     * @param tenantId       租户ID
     * @param orderId        订单ID
     * @param orderNo        订单号
     * @param bizNo          业务单号（幂等键）
     * @param discountAmount 实际折扣金额
     */
    void writeOffCoupon(Long userCouponId, Long tenantId, Long orderId, String orderNo, String bizNo, BigDecimal discountAmount);

    /**
     * 扫描并过期已到期的用户券。
     *
     * <p>由定时任务调用，将超过有效期的可用券标记为过期状态。</p>
     *
     * @param tenantId      租户ID（可空，为空则处理所有租户）
     * @param expireBefore  过期截止时间，早于此时间的券将被标记过期
     * @param bizNo         业务单号
     * @param expireReason  过期原因
     * @return 本次过期处理的券数量
     */
    int expireCoupons(Long tenantId, LocalDateTime expireBefore, String bizNo, String expireReason);
}
