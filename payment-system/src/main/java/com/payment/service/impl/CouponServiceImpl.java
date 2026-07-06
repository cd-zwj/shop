package com.payment.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.payment.common.BusinessException;
import com.payment.config.RabbitMQConfig;
import com.payment.dto.AppCouponReceiveVO;
import com.payment.dto.AppCouponTemplateVO;
import com.payment.dto.AppUserCouponVO;
import com.payment.dto.CouponScopeCreateDTO;
import com.payment.dto.CouponTemplateCreateDTO;
import com.payment.dto.pricing.CouponDiscountCandidateDTO;
import com.payment.dto.pricing.OrderPricingItemDTO;
import com.payment.entity.CouponLockRecord;
import com.payment.entity.CouponReceiveRecord;
import com.payment.entity.CouponReleaseRecord;
import com.payment.entity.CouponScope;
import com.payment.entity.CouponTemplate;
import com.payment.entity.CouponWriteOffRecord;
import com.payment.entity.CouponExpireRecord;
import com.payment.entity.MemberAccountTag;
import com.payment.entity.TenantMember;
import com.payment.entity.UserCoupon;
import com.payment.enums.CouponOwnerTypeEnum;
import com.payment.enums.CouponScopeTypeEnum;
import com.payment.enums.CouponTypeEnum;
import com.payment.enums.UserCouponStatusEnum;
import com.payment.mapper.CouponLockRecordMapper;
import com.payment.mapper.CouponReceiveRecordMapper;
import com.payment.mapper.CouponReleaseRecordMapper;
import com.payment.mapper.CouponScopeMapper;
import com.payment.mapper.CouponTemplateMapper;
import com.payment.mapper.CouponWriteOffRecordMapper;
import com.payment.mapper.CouponExpireRecordMapper;
import com.payment.mapper.MemberAccountTagMapper;
import com.payment.mapper.TenantMemberMapper;
import com.payment.mapper.UserCouponMapper;
import com.payment.service.CouponService;
import com.payment.service.OutboxPublisher;
import com.payment.service.UserBehaviorLogService;
import com.payment.service.outbox.OutboxMessageCommand;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.payment.util.BizNoGenerator;

/**
 * 优惠券服务实现类，覆盖优惠券完整生命周期管理。
 * <p>
 * 职责包括：
 * <ul>
 *   <li><b>模板管理</b>：创建优惠券模板、配置适用范围（商品/分类/商户/用户）、激活与禁用模板</li>
 *   <li><b>模板查询</b>：商户模板列表、平台模板列表、C端可用模板列表（含库存与领取状态）</li>
 *   <li><b>领取</b>：校验领取窗口、库存扣减（乐观锁）、生成用户优惠券并记录领取流水</li>
 *   <li><b>锁定</b>：下单时将优惠券从"已领取"锁定为"已锁定"，绑定订单号，防止重复使用</li>
 *   <li><b>释放</b>：订单取消/超时后将优惠券回退为"已领取"状态，恢复可用</li>
 *   <li><b>核销</b>：支付成功后将优惠券标记为"已使用"，记录实际抵扣金额</li>
 *   <li><b>过期</b>：批量处理过期优惠券，状态置为"已过期"</li>
 *   <li><b>定价候选</b>：订单计价时解析优惠券折扣候选，计算适用商品金额与优惠规则快照</li>
 * </ul>
 * <p>
 * 所有状态变更操作均通过 Outbox 模式发布事件到 RabbitMQ，保障消息最终一致性。
 *
 * @see com.payment.service.CouponService
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CouponServiceImpl implements CouponService {

    private static final String TEMPLATE_STATUS_DRAFT = "DRAFT";
    private static final String TEMPLATE_STATUS_ACTIVE = "ACTIVE";
    private static final String TEMPLATE_STATUS_DISABLED = "DISABLED";
    private static final String COUPON_EVENT_BIZ_TYPE = "COUPON_EVENT";
    private static final String COUPON_EVENT_MESSAGE_PREFIX = "COUPON";

    private final CouponTemplateMapper couponTemplateMapper;
    private final CouponScopeMapper couponScopeMapper;
    private final TenantMemberMapper tenantMemberMapper;
    private final MemberAccountTagMapper memberAccountTagMapper;
    private final UserCouponMapper userCouponMapper;
    private final CouponReceiveRecordMapper receiveRecordMapper;
    private final CouponLockRecordMapper lockRecordMapper;
    private final CouponReleaseRecordMapper releaseRecordMapper;
    private final CouponWriteOffRecordMapper writeOffRecordMapper;
    private final CouponExpireRecordMapper expireRecordMapper;
    private final UserBehaviorLogService userBehaviorLogService;
    private final OutboxPublisher outboxPublisher;

    /**
     * 查询指定商户的优惠券模板列表。
     *
     * @param tenantId 商户ID，不能为空
     * @param status   模板状态筛选条件（DRAFT/ACTIVE/DISABLED），可为 null 表示不筛选
     * @return 该商户下归属类型为 TENANT 的优惠券模板列表，按创建时间降序
     */
    @Override
    public List<CouponTemplate> listTemplates(Long tenantId, String status) {
        if (tenantId == null || tenantId <= 0) {
            throw new BusinessException("商户ID不能为空");
        }
        return couponTemplateMapper.selectList(new LambdaQueryWrapper<CouponTemplate>()
                .eq(CouponTemplate::getTemplateScope, CouponOwnerTypeEnum.TENANT.name())
                .eq(CouponTemplate::getTenantId, tenantId)
                .eq(CouponTemplate::getDeleted, 0)
                .eq(status != null && !status.isBlank(), CouponTemplate::getStatus, status)
                .orderByDesc(CouponTemplate::getCreateTime));
    }

    /**
     * 查询平台级别的优惠券模板列表。
     *
     * @param status 模板状态筛选条件（DRAFT/ACTIVE/DISABLED），可为 null 表示不筛选
     * @return 平台归属类型为 PLATFORM 的优惠券模板列表，按创建时间降序
     */
    @Override
    public List<CouponTemplate> listPlatformTemplates(String status) {
        return couponTemplateMapper.selectList(new LambdaQueryWrapper<CouponTemplate>()
                .eq(CouponTemplate::getTemplateScope, CouponOwnerTypeEnum.PLATFORM.name())
                .eq(CouponTemplate::getDeleted, 0)
                .eq(status != null && !status.isBlank(), CouponTemplate::getStatus, status)
                .orderByDesc(CouponTemplate::getCreateTime));
    }

    /**
     * 查询指定商户优惠券模板的适用范围列表。
     *
     * @param couponTemplateId 优惠券模板 ID
     * @param tenantId         商户 ID，用于校验模板归属
     * @return 适用范围列表
     * @throws BusinessException 模板不存在或不属于当前商户时抛出
     */
    @Override
    public List<CouponScope> listScopes(Long couponTemplateId, Long tenantId) {
        CouponTemplate template = requireTemplate(couponTemplateId);
        if (!Objects.equals(template.getTenantId(), tenantId)) {
            throw new BusinessException("优惠券模板不属于当前商户");
        }
        return couponScopeMapper.selectList(new LambdaQueryWrapper<CouponScope>()
                .eq(CouponScope::getCouponTemplateId, couponTemplateId)
                .eq(CouponScope::getDeleted, 0));
    }

    /**
     * 查询平台优惠券模板的适用范围列表。
     *
     * @param couponTemplateId 优惠券模板 ID
     * @return 适用范围列表
     * @throws BusinessException 模板不存在或不是平台券时抛出
     */
    @Override
    public List<CouponScope> listPlatformScopes(Long couponTemplateId) {
        CouponTemplate template = requireTemplate(couponTemplateId);
        if (!CouponOwnerTypeEnum.PLATFORM.name().equals(template.getTemplateScope())) {
            throw new BusinessException("优惠券模板不是平台券");
        }
        return couponScopeMapper.selectList(new LambdaQueryWrapper<CouponScope>()
                .eq(CouponScope::getCouponTemplateId, couponTemplateId)
                .eq(CouponScope::getDeleted, 0));
    }

    /**
     * 创建优惠券模板。
     * <p>
     * 校验参数合法性后创建草稿状态的模板，平台券不需要绑定商户。
     *
     * @param dto 模板创建参数
     * @return 新创建的优惠券模板实体（状态为 DRAFT）
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public CouponTemplate createTemplate(CouponTemplateCreateDTO dto) {
        validateTemplateCreate(dto);

        LocalDateTime now = LocalDateTime.now();
        CouponTemplate template = new CouponTemplate();
        template.setTemplateNo(BizNoGenerator.generate("CT"));
        template.setTenantId(CouponOwnerTypeEnum.PLATFORM.name().equals(dto.getTemplateScope()) ? null : dto.getTenantId());
        template.setTemplateScope(dto.getTemplateScope());
        template.setTemplateName(dto.getTemplateName().trim());
        template.setCouponType(dto.getCouponType());
        template.setThresholdAmount(defaultAmount(dto.getThresholdAmount()));
        template.setDiscountAmount(defaultAmount(dto.getDiscountAmount()));
        template.setDiscountRate(dto.getDiscountRate());
        template.setMaxDiscountAmount(dto.getMaxDiscountAmount());
        template.setTotalQuantity(defaultInt(dto.getTotalQuantity()));
        template.setReceivedQuantity(0);
        template.setUsedQuantity(0);
        template.setPerUserLimit(dto.getPerUserLimit() == null ? 1 : dto.getPerUserLimit());
        template.setReceiveStartTime(dto.getReceiveStartTime());
        template.setReceiveEndTime(dto.getReceiveEndTime());
        template.setValidType(dto.getValidDays() != null && dto.getValidDays() > 0 ? "FIXED_DAYS" : "FIXED_RANGE");
        template.setValidDays(dto.getValidDays());
        template.setValidStartTime(dto.getValidStartTime());
        template.setValidEndTime(dto.getValidEndTime());
        template.setCanStackBalance(Boolean.FALSE);
        template.setCanStackPoints(Boolean.FALSE);
        template.setCanStackOtherCoupon(Boolean.FALSE);
        template.setApplicableProductScope("ALL");
        template.setDescription(trimToNull(dto.getDescription()));
        template.setStatus(TEMPLATE_STATUS_DRAFT);
        template.setDeleted(0);
        template.setCreateTime(now);
        template.setUpdateTime(now);
        couponTemplateMapper.insert(template);
        return template;
    }

    /**
     * 为优惠券模板添加适用范围规则。
     *
     * @param dto 适用范围创建参数
     * @return 新创建的适用范围实体
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public CouponScope addScope(CouponScopeCreateDTO dto) {
        if (dto == null) {
            throw new BusinessException("优惠券适用范围不能为空");
        }
        CouponTemplate template = requireTemplate(dto.getCouponTemplateId());
        CouponScopeTypeEnum scopeType = parseEnum(CouponScopeTypeEnum.class, dto.getScopeType(), "优惠券适用范围类型不合法");
        validateScopeBoundary(template, dto, scopeType);

        CouponScope scope = new CouponScope();
        scope.setCouponTemplateId(template.getId());
        scope.setScopeType(scopeType.name());
        scope.setScopeId(dto.getScopeId());
        scope.setScopeCode(trimToNull(dto.getScopeCode()));
        scope.setTenantId(resolveScopeTenantId(template, dto, scopeType));
        scope.setDeleted(0);
        couponScopeMapper.insert(scope);
        return scope;
    }

    /**
     * 激活优惠券模板，将状态从 DRAFT 变更为 ACTIVE。
     *
     * @param couponTemplateId 优惠券模板 ID
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void activateTemplate(Long couponTemplateId) {
        CouponTemplate template = requireTemplate(couponTemplateId);
        validateTemplateForActivation(template);
        template.setStatus(TEMPLATE_STATUS_ACTIVE);
        template.setUpdateTime(LocalDateTime.now());
        couponTemplateMapper.updateById(template);
    }

    /**
     * 禁用优惠券模板，将状态变更为 DISABLED。
     *
     * @param couponTemplateId 优惠券模板 ID
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void disableTemplate(Long couponTemplateId) {
        CouponTemplate template = requireTemplate(couponTemplateId);
        template.setStatus(TEMPLATE_STATUS_DISABLED);
        template.setUpdateTime(LocalDateTime.now());
        couponTemplateMapper.updateById(template);
    }

    /**
     * 查询 C 端用户可见的可用优惠券模板列表。
     * <p>
     * 包含平台券和当前商户券，在领取时间窗口内、库存充足的模板。
     * 批量预加载 scope 消除 N+1 查询。
     *
     * @param tenantId       商户 ID
     * @param platformUserId 平台用户 ID
     * @return 可领取的优惠券模板 VO 列表（含库存和领取状态）
     */
    @Override
    public List<AppCouponTemplateVO> listAvailableTemplates(Long tenantId, Long platformUserId) {
        LocalDateTime now = LocalDateTime.now();
        List<CouponTemplate> templates = couponTemplateMapper.selectList(new LambdaQueryWrapper<CouponTemplate>()
                .eq(CouponTemplate::getStatus, "ACTIVE")
                .eq(CouponTemplate::getDeleted, 0)
                .and(wrapper -> wrapper
                        .eq(CouponTemplate::getTemplateScope, CouponOwnerTypeEnum.PLATFORM.name())
                        .or(inner -> inner
                                .eq(CouponTemplate::getTemplateScope, CouponOwnerTypeEnum.TENANT.name())
                                .eq(CouponTemplate::getTenantId, tenantId)))
                .and(wrapper -> wrapper
                        .isNull(CouponTemplate::getReceiveStartTime)
                        .or()
                        .le(CouponTemplate::getReceiveStartTime, now))
                .and(wrapper -> wrapper
                        .isNull(CouponTemplate::getReceiveEndTime)
                        .or()
                        .ge(CouponTemplate::getReceiveEndTime, now))
                .orderByDesc(CouponTemplate::getCreateTime));
        if (templates == null || templates.isEmpty()) {
            return Collections.emptyList();
        }

        // 批量预加载所有模板的 scope，避免 N+1 查询
        Set<Long> templateIds = templates.stream()
                .map(CouponTemplate::getId)
                .collect(Collectors.toCollection(HashSet::new));
        Map<Long, List<CouponScope>> scopesMap = batchLoadScopes(templateIds);

        return templates.stream()
                .filter(template -> {
                    List<CouponScope> scopes = scopesMap.getOrDefault(template.getId(), Collections.emptyList());
                    return isVisibleWithScopes(template, tenantId, platformUserId, scopes);
                })
                .map(template -> toTemplateVO(template, platformUserId))
                .collect(Collectors.toList());
    }

    /**
     * 查询用户的优惠券列表，支持按状态筛选。
     * <p>
     * 包含当前商户的优惠券和平台级优惠券，平台券通过 scope 校验可见性。
     * 批量加载模板信息避免 N+1 查询。
     *
     * @param tenantId       商户 ID
     * @param platformUserId 平台用户 ID
     * @param status         优惠券状态筛选（RECEIVED/LOCKED/USED/EXPIRED），可为 null
     * @return 用户优惠券 VO 列表
     */
    @Override
    public List<AppUserCouponVO> listUserCoupons(Long tenantId, Long platformUserId, String status) {
        List<UserCoupon> coupons = userCouponMapper.selectList(new LambdaQueryWrapper<UserCoupon>()
                .and(w -> w.eq(UserCoupon::getTenantId, tenantId).or().isNull(UserCoupon::getTenantId))
                .eq(UserCoupon::getPlatformUserId, platformUserId)
                .eq(status != null && !status.isBlank(), UserCoupon::getCouponStatus, status)
                .orderByDesc(UserCoupon::getCreateTime));
        if (coupons == null || coupons.isEmpty()) {
            return Collections.emptyList();
        }

        Set<Long> templateIds = coupons.stream()
                .map(UserCoupon::getTemplateId)
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(HashSet::new));
        Map<Long, CouponTemplate> templateMap = templateIds.isEmpty()
                ? Collections.emptyMap()
                : couponTemplateMapper.selectBatchIds(templateIds).stream()
                        .collect(Collectors.toMap(CouponTemplate::getId, Function.identity()));

        return coupons.stream()
                .filter(coupon -> {
                    CouponTemplate tpl = templateMap.get(coupon.getTemplateId());
                    if (tpl == null) {
                        return false;
                    }
                    if (!CouponOwnerTypeEnum.PLATFORM.name().equals(tpl.getTemplateScope())) {
                        return true;
                    }
                    return isCouponTemplateVisibleToTenant(tpl, tenantId, platformUserId);
                })
                .map(coupon -> toUserCouponVO(coupon, templateMap.get(coupon.getTemplateId())))
                .collect(Collectors.toList());
    }

    /**
     * 领取优惠券。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public UserCoupon receiveCoupon(Long couponTemplateId, Long tenantId, Long platformUserId, String bizNo) {
        CouponTemplate template = couponTemplateMapper.selectById(couponTemplateId);
        if (template == null || Integer.valueOf(1).equals(template.getDeleted())) {
            throw new BusinessException("优惠券模板不存在");
        }
        if (!"ACTIVE".equals(template.getStatus())) {
            throw new BusinessException("优惠券模板未生效");
        }
        ensureTemplateTenant(template, tenantId, platformUserId);
        ensureMemberRules(template, tenantId, platformUserId);
        ensureReceiveWindow(template);

        int affected = userCouponMapper.claimCouponSlot(template.getId(), platformUserId);
        if (affected == 0) {
            throw new BusinessException("优惠券库存不足或已超过每人限领数量");
        }

        LocalDateTime now = LocalDateTime.now();
        UserCoupon coupon = new UserCoupon();
        coupon.setCouponNo(BizNoGenerator.generate("UC"));
        coupon.setTemplateId(template.getId());
        coupon.setTenantId(resolveCouponTenantId(template, tenantId));
        coupon.setPlatformUserId(platformUserId);
        coupon.setSourceType("RECEIVE");
        coupon.setCouponStatus(UserCouponStatusEnum.RECEIVED.name());
        coupon.setReceiveTime(now);
        coupon.setExpireTime(resolveExpireTime(template, now));
        coupon.setVersion(0);
        coupon.setCreateTime(now);
        coupon.setUpdateTime(now);
        userCouponMapper.insert(coupon);

        CouponReceiveRecord record = new CouponReceiveRecord();
        record.setUserCouponId(coupon.getId());
        record.setCouponTemplateId(template.getId());
        record.setTenantId(coupon.getTenantId());
        record.setPlatformUserId(platformUserId);
        record.setBizNo(bizNo);
        record.setReceiveTime(now);
        receiveRecordMapper.insert(record);
        publishCouponEvent("RECEIVED", coupon, bizNo, null);
        return coupon;
    }

    /**
     * C 端用户领取优惠券（带返回 VO 和行为日志记录）。
     *
     * @param couponTemplateId 优惠券模板 ID
     * @param tenantId         商户 ID
     * @param platformUserId   平台用户 ID
     * @return 领取结果 VO，包含用户优惠券信息
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public AppCouponReceiveVO receiveCouponForApp(Long couponTemplateId, Long tenantId, Long platformUserId) {
        UserCoupon coupon = receiveCoupon(couponTemplateId, tenantId, platformUserId, BizNoGenerator.generate("CR"));
        AppCouponReceiveVO result = new AppCouponReceiveVO();
        result.setUserCouponId(coupon.getId());
        result.setCouponNo(coupon.getCouponNo());
        result.setTemplateId(coupon.getTemplateId());
        result.setTenantId(coupon.getTenantId());
        result.setCouponStatus(coupon.getCouponStatus());
        result.setExpireTime(coupon.getExpireTime());

        // 记录优惠券领取行为（埋点失败不影响主流程）
        try {
            userBehaviorLogService.recordBehavior(
                    platformUserId, tenantId, "FAVORITE",
                    "COUPON", couponTemplateId,
                    "{\"userCouponId\":" + coupon.getId() + ",\"couponNo\":\"" + coupon.getCouponNo() + "\"}");
        } catch (Exception e) {
            log.warn("记录 COUPON 领取行为日志失败, templateId={}", couponTemplateId, e);
        }

        return result;
    }

    /**
     * 解析优惠券折扣候选，供订单定价引擎使用。
     * <p>
     * 校验优惠券归属、状态、过期时间和适用范围后，计算适用商品金额并构建折扣候选 DTO。
     *
     * @param userCouponId   用户优惠券 ID，为 null 时返回 null
     * @param tenantId       商户 ID
     * @param platformUserId 平台用户 ID
     * @param items          订单商品列表
     * @return 优惠券折扣候选 DTO，优惠券不可用时返回 null
     */
    @Override
    public CouponDiscountCandidateDTO resolveCouponCandidate(Long userCouponId,
                                                             Long tenantId,
                                                             Long platformUserId,
                                                             List<OrderPricingItemDTO> items) {
        if (userCouponId == null) {
            return null;
        }
        UserCoupon userCoupon = requireCoupon(userCouponId);
        ensureTenant(userCoupon, tenantId);
        ensureUser(userCoupon, platformUserId);
        if (!UserCouponStatusEnum.RECEIVED.name().equals(userCoupon.getCouponStatus())) {
            throw new BusinessException("选择的优惠券不可用");
        }
        if (userCoupon.getExpireTime() != null && userCoupon.getExpireTime().isBefore(LocalDateTime.now())) {
            throw new BusinessException("选择的优惠券已过期");
        }

        CouponTemplate template = couponTemplateMapper.selectById(userCoupon.getTemplateId());
        if (template == null || Integer.valueOf(1).equals(template.getDeleted())) {
            throw new BusinessException("选择的优惠券模板不存在");
        }
        if (!"ACTIVE".equals(template.getStatus())) {
            throw new BusinessException("选择的优惠券模板未生效");
        }
        ensureTemplateTenant(template, tenantId, platformUserId);
        ensureMemberRules(template, tenantId, platformUserId);

        BigDecimal eligibleAmount = calculateEligibleAmount(template.getId(), tenantId, platformUserId, items);
        if (eligibleAmount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException("选择的优惠券不适用于当前订单");
        }

        CouponDiscountCandidateDTO candidate = new CouponDiscountCandidateDTO();
        candidate.setUserCouponId(userCoupon.getId());
        candidate.setCouponTemplateId(template.getId());
        candidate.setCouponType(template.getCouponType());
        candidate.setCouponStatus(userCoupon.getCouponStatus());
        candidate.setEligibleAmount(eligibleAmount);
        candidate.setThresholdAmount(template.getThresholdAmount());
        candidate.setDiscountAmount(template.getDiscountAmount());
        candidate.setDiscountRate(template.getDiscountRate());
        candidate.setMaxDiscountAmount(template.getMaxDiscountAmount());
        candidate.setCanStackBalance(template.getCanStackBalance());
        candidate.setCanStackPoints(template.getCanStackPoints());
        candidate.setCanStackOtherCoupon(template.getCanStackOtherCoupon());
        candidate.setRuleSnapshotJson("{\"templateNo\":\"" + template.getTemplateNo() + "\",\"couponType\":\"" + template.getCouponType() + "\"}");
        return candidate;
    }

    /**
     * 锁定用户券。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void lockCoupon(Long userCouponId, Long tenantId, Long platformUserId, Long orderId, String orderNo, String bizNo) {
        UserCoupon coupon = requireCoupon(userCouponId);
        ensureTenant(coupon, tenantId);
        ensureUser(coupon, platformUserId);
        if (UserCouponStatusEnum.LOCKED.name().equals(coupon.getCouponStatus())
                && Objects.equals(coupon.getOrderNo(), orderNo)) {
            return;
        }
        if (!UserCouponStatusEnum.RECEIVED.name().equals(coupon.getCouponStatus())) {
            throw new BusinessException("优惠券不可锁定");
        }
        if (coupon.getExpireTime() != null && coupon.getExpireTime().isBefore(LocalDateTime.now())) {
            throw new BusinessException("优惠券已过期");
        }

        LocalDateTime now = LocalDateTime.now();
        coupon.setCouponStatus(UserCouponStatusEnum.LOCKED.name());
        coupon.setOrderNo(orderNo);
        coupon.setLockTime(now);
        coupon.setUpdateTime(now);
        int affected = userCouponMapper.updateById(coupon);
        if (affected == 0) {
            throw new BusinessException("优惠券锁定失败，请重试");
        }

        CouponLockRecord record = new CouponLockRecord();
        record.setUserCouponId(userCouponId);
        record.setTenantId(tenantId);
        record.setOrderId(orderId);
        record.setOrderNo(orderNo);
        record.setBizNo(bizNo);
        record.setLockTime(now);
        record.setLockStatus(UserCouponStatusEnum.LOCKED.name());
        lockRecordMapper.insert(record);
        publishCouponEvent("LOCKED", coupon, bizNo, orderNo);
    }

    /**
     * 释放用户券。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void releaseCoupon(Long userCouponId, Long tenantId, Long platformUserId, Long orderId, String orderNo, String bizNo, String releaseReason) {
        UserCoupon coupon = requireCoupon(userCouponId);
        ensureTenant(coupon, tenantId);
        ensureUser(coupon, platformUserId);
        if (!UserCouponStatusEnum.LOCKED.name().equals(coupon.getCouponStatus())) {
            return;
        }
        if (orderNo != null && !Objects.equals(coupon.getOrderNo(), orderNo)) {
            throw new BusinessException("优惠券不属于当前订单");
        }

        LocalDateTime now = LocalDateTime.now();
        coupon.setCouponStatus(UserCouponStatusEnum.RECEIVED.name());
        coupon.setOrderNo(null);
        coupon.setLockTime(null);
        coupon.setUpdateTime(now);
        int affected = userCouponMapper.updateById(coupon);
        if (affected == 0) {
            throw new BusinessException("优惠券释放失败，请重试");
        }

        CouponReleaseRecord record = new CouponReleaseRecord();
        record.setUserCouponId(userCouponId);
        record.setTenantId(tenantId);
        record.setOrderId(orderId);
        record.setOrderNo(orderNo);
        record.setBizNo(bizNo);
        record.setReleaseReason(releaseReason);
        record.setReleaseTime(now);
        releaseRecordMapper.insert(record);
        publishCouponEvent("RELEASED", coupon, bizNo, orderNo);
    }

    /**
     * 核销用户券。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void writeOffCoupon(Long userCouponId, Long tenantId, Long orderId, String orderNo, String bizNo, BigDecimal discountAmount) {
        UserCoupon coupon = requireCoupon(userCouponId);
        ensureTenant(coupon, tenantId);
        if (!UserCouponStatusEnum.LOCKED.name().equals(coupon.getCouponStatus())) {
            throw new BusinessException("优惠券未锁定，不能核销");
        }
        if (orderNo != null && !Objects.equals(coupon.getOrderNo(), orderNo)) {
            throw new BusinessException("优惠券不属于当前订单，不能核销");
        }
        if (discountAmount == null || discountAmount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException("优惠券核销金额必须大于0");
        }

        LocalDateTime now = LocalDateTime.now();
        coupon.setCouponStatus(UserCouponStatusEnum.USED.name());
        coupon.setUseTime(now);
        coupon.setUpdateTime(now);
        int affected = userCouponMapper.updateById(coupon);
        if (affected == 0) {
            throw new BusinessException("优惠券核销失败，请重试");
        }

        CouponWriteOffRecord record = new CouponWriteOffRecord();
        record.setUserCouponId(userCouponId);
        record.setCouponTemplateId(coupon.getTemplateId());
        record.setTenantId(tenantId);
        record.setOrderId(orderId);
        record.setOrderNo(orderNo);
        record.setBizNo(bizNo);
        record.setDiscountAmount(discountAmount);
        record.setWriteOffTime(now);
        writeOffRecordMapper.insert(record);
        publishCouponEvent("USED", coupon, bizNo, orderNo);
    }

    /**
     * 批量处理过期优惠券，将过期状态置为 EXPIRED。
     *
     * @param tenantId      商户 ID，为 null 时处理所有商户的过期券
     * @param expireBefore  过期截止时间，为 null 时取当前时间
     * @param bizNo         业务单号，用于事件追踪
     * @param expireReason  过期原因
     * @return 成功标记为过期的优惠券数量
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public int expireCoupons(Long tenantId, LocalDateTime expireBefore, String bizNo, String expireReason) {
        LocalDateTime cutoff = expireBefore == null ? LocalDateTime.now() : expireBefore;
        List<UserCoupon> coupons = userCouponMapper.selectList(new LambdaQueryWrapper<UserCoupon>()
                .eq(UserCoupon::getCouponStatus, UserCouponStatusEnum.RECEIVED.name())
                .le(UserCoupon::getExpireTime, cutoff)
                .eq(tenantId != null, UserCoupon::getTenantId, tenantId));
        if (coupons == null || coupons.isEmpty()) {
            return 0;
        }

        LocalDateTime now = LocalDateTime.now();
        int expiredCount = 0;
        for (UserCoupon coupon : coupons) {
            coupon.setCouponStatus(UserCouponStatusEnum.EXPIRED.name());
            coupon.setOrderNo(null);
            coupon.setLockTime(null);
            coupon.setUpdateTime(now);
            int affected = userCouponMapper.updateById(coupon);
            if (affected == 0) {
                continue;
            }
            expiredCount++;

            CouponExpireRecord record = new CouponExpireRecord();
            record.setUserCouponId(coupon.getId());
            record.setCouponTemplateId(coupon.getTemplateId());
            record.setTenantId(coupon.getTenantId());
            record.setPlatformUserId(coupon.getPlatformUserId());
            record.setBizNo(resolveExpireBizNo(bizNo, coupon));
            record.setExpireReason(expireReason);
            record.setExpireTime(now);
            expireRecordMapper.insert(record);
            publishCouponEvent("EXPIRED", coupon, record.getBizNo(), null);
        }
        return expiredCount;
    }

    /**
     * 发布优惠券状态变更事件到 Outbox。
     *
     * @param eventType 事件类型（RECEIVED/LOCKED/RELEASED/USED/EXPIRED）
     * @param coupon    用户优惠券实体
     * @param bizNo     业务单号
     * @param orderNo   订单号，可为 null
     */
    private void publishCouponEvent(String eventType, UserCoupon coupon, String bizNo, String orderNo) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("bizType", COUPON_EVENT_BIZ_TYPE);
        body.put("eventType", eventType);
        body.put("bizNo", resolveCouponEventBizNo(eventType, coupon, bizNo));
        body.put("userCouponId", coupon.getId());
        body.put("couponNo", coupon.getCouponNo());
        body.put("couponTemplateId", coupon.getTemplateId());
        body.put("tenantId", coupon.getTenantId());
        body.put("platformUserId", coupon.getPlatformUserId());
        body.put("couponStatus", coupon.getCouponStatus());
        body.put("orderNo", orderNo);

        outboxPublisher.publish(OutboxMessageCommand.builder()
                .messagePrefix(COUPON_EVENT_MESSAGE_PREFIX)
                .bizType(COUPON_EVENT_BIZ_TYPE)
                .bizNo((String) body.get("bizNo"))
                .routingKey(RabbitMQConfig.COUPON_EVENT_QUEUE)
                .messageBody(body)
                .build());
    }

    /** 解析优惠券事件的业务单号，优先使用入参 bizNo，否则使用优惠券编号。 */
    private String resolveCouponEventBizNo(String eventType, UserCoupon coupon, String bizNo) {
        if (bizNo != null && !bizNo.isBlank()) {
            return bizNo;
        }
        if (coupon.getCouponNo() != null && !coupon.getCouponNo().isBlank()) {
            return coupon.getCouponNo();
        }
        return "COUPON_" + eventType + "_" + coupon.getId();
    }

    /** 校验优惠券模板创建参数合法性。 */
    private void validateTemplateCreate(CouponTemplateCreateDTO dto) {
        if (dto == null) {
            throw new BusinessException("优惠券模板不能为空");
        }
        CouponOwnerTypeEnum templateScope = parseEnum(CouponOwnerTypeEnum.class, dto.getTemplateScope(), "优惠券归属类型不合法");
        CouponTypeEnum couponType = parseEnum(CouponTypeEnum.class, dto.getCouponType(), "优惠券类型不合法");
        if (dto.getTemplateName() == null || dto.getTemplateName().isBlank()) {
            throw new BusinessException("优惠券名称不能为空");
        }
        if (templateScope == CouponOwnerTypeEnum.TENANT && (dto.getTenantId() == null || dto.getTenantId() <= 0)) {
            throw new BusinessException("商户券必须绑定商户");
        }
        if (defaultAmount(dto.getThresholdAmount()).compareTo(BigDecimal.ZERO) < 0) {
            throw new BusinessException("优惠券使用门槛不能小于0");
        }
        if (defaultAmount(dto.getDiscountAmount()).compareTo(BigDecimal.ZERO) < 0) {
            throw new BusinessException("优惠券优惠金额不能小于0");
        }
        if (dto.getTotalQuantity() != null && dto.getTotalQuantity() < 0) {
            throw new BusinessException("优惠券库存不能小于0");
        }
        if (dto.getPerUserLimit() != null && dto.getPerUserLimit() < 0) {
            throw new BusinessException("每人限领数量不能小于0");
        }
        validateReceiveWindow(dto.getReceiveStartTime(), dto.getReceiveEndTime());
        validateValidity(dto.getValidDays(), dto.getValidStartTime(), dto.getValidEndTime());
        validateCouponRule(couponType, dto);
    }

    /** 校验优惠券模板是否满足激活条件（复用创建校验逻辑）。 */
    private void validateTemplateForActivation(CouponTemplate template) {
        CouponTemplateCreateDTO dto = new CouponTemplateCreateDTO();
        dto.setTenantId(template.getTenantId());
        dto.setTemplateScope(template.getTemplateScope());
        dto.setTemplateName(template.getTemplateName());
        dto.setCouponType(template.getCouponType());
        dto.setThresholdAmount(template.getThresholdAmount());
        dto.setDiscountAmount(template.getDiscountAmount());
        dto.setDiscountRate(template.getDiscountRate());
        dto.setMaxDiscountAmount(template.getMaxDiscountAmount());
        dto.setTotalQuantity(template.getTotalQuantity());
        dto.setPerUserLimit(template.getPerUserLimit());
        dto.setReceiveStartTime(template.getReceiveStartTime());
        dto.setReceiveEndTime(template.getReceiveEndTime());
        dto.setValidDays(template.getValidDays());
        dto.setValidStartTime(template.getValidStartTime());
        dto.setValidEndTime(template.getValidEndTime());
        validateTemplateCreate(dto);
    }

    /** 校验优惠券规则参数（满减/无门槛/折扣各自的约束条件）。 */
    private void validateCouponRule(CouponTypeEnum couponType, CouponTemplateCreateDTO dto) {
        BigDecimal thresholdAmount = defaultAmount(dto.getThresholdAmount());
        BigDecimal discountAmount = defaultAmount(dto.getDiscountAmount());
        if (couponType == CouponTypeEnum.FULL_REDUCTION) {
            if (thresholdAmount.compareTo(BigDecimal.ZERO) <= 0 || discountAmount.compareTo(BigDecimal.ZERO) <= 0) {
                throw new BusinessException("满减券必须配置正数门槛和优惠金额");
            }
            return;
        }
        if (couponType == CouponTypeEnum.NO_THRESHOLD || couponType == CouponTypeEnum.RECHARGE_GIFT) {
            if (discountAmount.compareTo(BigDecimal.ZERO) <= 0) {
                throw new BusinessException("无门槛券或充值赠券必须配置正数优惠金额");
            }
            return;
        }
        if (couponType == CouponTypeEnum.DISCOUNT_RATE) {
            BigDecimal rate = dto.getDiscountRate();
            if (rate == null || rate.compareTo(BigDecimal.ZERO) <= 0 || rate.compareTo(BigDecimal.ONE) > 0) {
                throw new BusinessException("折扣券折扣比例必须在0到1之间");
            }
            if (dto.getMaxDiscountAmount() != null && dto.getMaxDiscountAmount().compareTo(BigDecimal.ZERO) < 0) {
                throw new BusinessException("折扣券最高优惠金额不能小于0");
            }
        }
    }

    private void validateReceiveWindow(LocalDateTime receiveStartTime, LocalDateTime receiveEndTime) {
        if (receiveStartTime != null && receiveEndTime != null && receiveStartTime.isAfter(receiveEndTime)) {
            throw new BusinessException("优惠券领取开始时间不能晚于结束时间");
        }
    }

    private void validateValidity(Integer validDays,
                                  LocalDateTime validStartTime,
                                  LocalDateTime validEndTime) {
        boolean hasRelativeValidity = validDays != null && validDays > 0;
        boolean hasFixedValidity = validEndTime != null;
        if (!hasRelativeValidity && !hasFixedValidity) {
            throw new BusinessException("优惠券有效期未配置");
        }
        if (validDays != null && validDays <= 0) {
            throw new BusinessException("领取后有效天数必须大于0");
        }
        if (validStartTime != null && validEndTime != null && validStartTime.isAfter(validEndTime)) {
            throw new BusinessException("优惠券固定有效期开始时间不能晚于结束时间");
        }
    }

    private void validateScopeBoundary(CouponTemplate template, CouponScopeCreateDTO dto, CouponScopeTypeEnum scopeType) {
        if (CouponOwnerTypeEnum.TENANT.name().equals(template.getTemplateScope())
                && !Objects.equals(template.getTenantId(), dto.getTenantId())) {
            throw new BusinessException("商户券适用范围必须归属同一商户");
        }
        if ((scopeType == CouponScopeTypeEnum.TENANT || scopeType == CouponScopeTypeEnum.PRODUCT
                || scopeType == CouponScopeTypeEnum.USER)
                && (dto.getScopeId() == null || dto.getScopeId() <= 0)) {
            throw new BusinessException("优惠券适用范围目标ID不能为空");
        }
        if (scopeType == CouponScopeTypeEnum.CATEGORY && (dto.getScopeCode() == null || dto.getScopeCode().isBlank())) {
            throw new BusinessException("优惠券分类适用范围编码不能为空");
        }
    }

    private Long resolveScopeTenantId(CouponTemplate template, CouponScopeCreateDTO dto, CouponScopeTypeEnum scopeType) {
        if (CouponOwnerTypeEnum.TENANT.name().equals(template.getTemplateScope())) {
            return template.getTenantId();
        }
        if (scopeType == CouponScopeTypeEnum.TENANT) {
            return dto.getScopeId();
        }
        return dto.getTenantId();
    }

    private CouponTemplate requireTemplate(Long couponTemplateId) {
        if (couponTemplateId == null || couponTemplateId <= 0) {
            throw new BusinessException("优惠券模板ID不能为空");
        }
        CouponTemplate template = couponTemplateMapper.selectById(couponTemplateId);
        if (template == null || Integer.valueOf(1).equals(template.getDeleted())) {
            throw new BusinessException("优惠券模板不存在");
        }
        return template;
    }

    private <E extends Enum<E>> E parseEnum(Class<E> enumType, String value, String message) {
        if (value == null || value.isBlank()) {
            throw new BusinessException(message);
        }
        try {
            return Enum.valueOf(enumType, value.trim());
        } catch (IllegalArgumentException e) {
            throw new BusinessException(message);
        }
    }

    private UserCoupon requireCoupon(Long userCouponId) {
        UserCoupon coupon = userCouponMapper.selectById(userCouponId);
        if (coupon == null) {
            throw new BusinessException("优惠券不存在");
        }
        return coupon;
    }

    /** 解析过期事件的业务单号，优先使用入参，否则生成 COUPON_EXPIRE_{id}。 */
    private String resolveExpireBizNo(String bizNo, UserCoupon coupon) {
        if (bizNo != null && !bizNo.isBlank()) {
            return bizNo;
        }
        return "COUPON_EXPIRE_" + coupon.getId();
    }

    private void ensureTenant(UserCoupon coupon, Long tenantId) {
        if (coupon.getTenantId() == null) {
            return;
        }
        if (!Objects.equals(coupon.getTenantId(), tenantId)) {
            throw new BusinessException("优惠券不属于当前商户");
        }
    }

    private void ensureUser(UserCoupon coupon, Long platformUserId) {
        if (!Objects.equals(coupon.getPlatformUserId(), platformUserId)) {
            throw new BusinessException("优惠券不属于当前用户");
        }
    }

    private void ensureTemplateTenant(CouponTemplate template, Long tenantId, Long platformUserId) {
        if (CouponOwnerTypeEnum.TENANT.name().equals(template.getTemplateScope()) && !Objects.equals(template.getTenantId(), tenantId)) {
            throw new BusinessException("优惠券模板不属于当前商户");
        }
        if (CouponOwnerTypeEnum.PLATFORM.name().equals(template.getTemplateScope())
                && !isTemplateVisibleForTenantAndUser(template, tenantId, platformUserId)) {
            throw new BusinessException("平台优惠券不适用于当前商户");
        }
    }

    private void ensureMemberRules(CouponTemplate template, Long tenantId, Long platformUserId) {
        // Member level / tag restrictions removed from entity — no-op for now
    }

    private Long resolveCouponTenantId(CouponTemplate template, Long tenantId) {
        if (CouponOwnerTypeEnum.PLATFORM.name().equals(template.getTemplateScope())) {
            return null;
        }
        return template.getTenantId();
    }

    private void ensureReceiveWindow(CouponTemplate template) {
        LocalDateTime now = LocalDateTime.now();
        if (template.getReceiveStartTime() != null && now.isBefore(template.getReceiveStartTime())) {
            throw new BusinessException("优惠券尚未开始领取");
        }
        if (template.getReceiveEndTime() != null && now.isAfter(template.getReceiveEndTime())) {
            throw new BusinessException("优惠券领取已结束");
        }
    }

    private LocalDateTime resolveExpireTime(CouponTemplate template, LocalDateTime receiveTime) {
        if (template.getValidDays() != null && template.getValidDays() > 0) {
            return receiveTime.plusDays(template.getValidDays());
        }
        if (template.getValidEndTime() != null) {
            return template.getValidEndTime();
        }
        throw new BusinessException("优惠券有效期未配置");
    }

    private int defaultInt(Integer value) {
        return value == null ? 0 : value;
    }

    private BigDecimal defaultAmount(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private String trimToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    private AppCouponTemplateVO toTemplateVO(CouponTemplate template, Long platformUserId) {
        AppCouponTemplateVO vo = new AppCouponTemplateVO();
        vo.setId(template.getId());
        vo.setTenantId(template.getTenantId());
        vo.setTemplateScope(template.getTemplateScope());
        vo.setTemplateName(template.getTemplateName());
        vo.setCouponType(template.getCouponType());
        vo.setThresholdAmount(template.getThresholdAmount());
        vo.setDiscountAmount(template.getDiscountAmount());
        vo.setDiscountRate(template.getDiscountRate());
        vo.setMaxDiscountAmount(template.getMaxDiscountAmount());
        vo.setPerUserLimit(template.getPerUserLimit());
        vo.setRemainingStock(resolveRemainingStock(template));
        int receivedByUser = countReceivedByUser(template.getId(), platformUserId);
        vo.setReceivedByCurrentUser(receivedByUser);
        vo.setReceivable(isReceivable(template, receivedByUser));
        vo.setReceiveStartTime(template.getReceiveStartTime());
        vo.setReceiveEndTime(template.getReceiveEndTime());
        vo.setValidStartTime(template.getValidStartTime());
        vo.setValidEndTime(template.getValidEndTime());
        vo.setValidDays(template.getValidDays());
        vo.setDescription(template.getDescription());
        return vo;
    }

    private AppUserCouponVO toUserCouponVO(UserCoupon coupon, CouponTemplate template) {
        AppUserCouponVO vo = new AppUserCouponVO();
        vo.setId(coupon.getId());
        vo.setCouponNo(coupon.getCouponNo());
        vo.setTemplateId(coupon.getTemplateId());
        vo.setTenantId(coupon.getTenantId());
        vo.setCouponStatus(coupon.getCouponStatus());
        vo.setOrderNo(coupon.getOrderNo());
        vo.setReceiveTime(coupon.getReceiveTime());
        vo.setExpireTime(coupon.getExpireTime());
        vo.setUseTime(coupon.getUseTime());
        if (template != null) {
            vo.setTemplateName(template.getTemplateName());
            vo.setCouponType(template.getCouponType());
            vo.setThresholdAmount(template.getThresholdAmount());
            vo.setDiscountAmount(template.getDiscountAmount());
            vo.setDiscountRate(template.getDiscountRate());
            vo.setMaxDiscountAmount(template.getMaxDiscountAmount());
        }
        return vo;
    }

    private boolean isReceivable(CouponTemplate template, int receivedByUser) {
        int perUserLimit = defaultInt(template.getPerUserLimit());
        return resolveRemainingStock(template) != 0 && (perUserLimit <= 0 || receivedByUser < perUserLimit);
    }

    private Integer resolveRemainingStock(CouponTemplate template) {
        int totalQuantity = defaultInt(template.getTotalQuantity());
        if (totalQuantity <= 0) {
            return -1;
        }
        return Math.max(0, totalQuantity - defaultInt(template.getReceivedQuantity()));
    }

    private int countReceivedByUser(Long templateId, Long platformUserId) {
        Long count = userCouponMapper.selectCount(new LambdaQueryWrapper<UserCoupon>()
                .eq(UserCoupon::getTemplateId, templateId)
                .eq(UserCoupon::getPlatformUserId, platformUserId));
        return count == null ? 0 : count.intValue();
    }

    private boolean isCouponTemplateVisibleToTenant(CouponTemplate template, Long tenantId, Long platformUserId) {
        List<CouponScope> scopes = listScopes(template.getId());
        return isVisibleWithScopes(template, tenantId, platformUserId, scopes);
    }

    private boolean isTemplateVisibleForTenantAndUser(CouponTemplate template, Long tenantId, Long platformUserId) {
        List<CouponScope> scopes = listScopes(template.getId());
        return isVisibleWithScopes(template, tenantId, platformUserId, scopes);
    }

    private boolean isVisibleWithScopes(CouponTemplate template, Long tenantId, Long platformUserId, List<CouponScope> scopes) {
        if (scopes.isEmpty()) {
            return true;
        }
        boolean hasTenantScope = false;
        boolean hasUserScope = false;
        boolean tenantMatched = false;
        boolean userMatched = false;
        Set<Long> scopedTenantIds = new HashSet<>();
        for (CouponScope scope : scopes) {
            if (CouponScopeTypeEnum.TENANT.name().equals(scope.getScopeType())) {
                hasTenantScope = true;
                if (Objects.equals(scope.getScopeId(), tenantId)) {
                    tenantMatched = true;
                }
            }
            if (CouponScopeTypeEnum.USER.name().equals(scope.getScopeType())) {
                hasUserScope = true;
                if (platformUserId != null && Objects.equals(scope.getScopeId(), platformUserId)) {
                    userMatched = true;
                }
            }
            if ((CouponScopeTypeEnum.PRODUCT.name().equals(scope.getScopeType())
                    || CouponScopeTypeEnum.CATEGORY.name().equals(scope.getScopeType()))
                    && scope.getTenantId() != null) {
                scopedTenantIds.add(scope.getTenantId());
            }
        }
        if (!scopedTenantIds.isEmpty() && !scopedTenantIds.contains(tenantId)) {
            return false;
        }
        return (!hasTenantScope || tenantMatched) && (!hasUserScope || userMatched);
    }

    private BigDecimal calculateEligibleAmount(Long couponTemplateId,
                                               Long tenantId,
                                               Long platformUserId,
                                               List<OrderPricingItemDTO> items) {
        BigDecimal orderAmount = sumItems(items);
        List<CouponScope> scopes = listScopes(couponTemplateId);
        if (scopes.isEmpty()) {
            return orderAmount;
        }

        Set<Long> productScopeIds = new HashSet<>();
        Set<String> categoryScopeCodes = new HashSet<>();
        Set<Long> scopedTenantIds = new HashSet<>();
        boolean hasTenantScope = false;
        boolean hasUserScope = false;
        boolean tenantMatched = false;
        boolean userMatched = false;
        boolean hasItemScope = false;
        for (CouponScope scope : scopes) {
            if (CouponScopeTypeEnum.TENANT.name().equals(scope.getScopeType())) {
                hasTenantScope = true;
                tenantMatched = tenantMatched || Objects.equals(scope.getScopeId(), tenantId);
            } else if (CouponScopeTypeEnum.USER.name().equals(scope.getScopeType())
                    && Objects.equals(scope.getScopeId(), platformUserId)) {
                hasUserScope = true;
                userMatched = true;
            } else if (CouponScopeTypeEnum.USER.name().equals(scope.getScopeType())) {
                hasUserScope = true;
            } else if (CouponScopeTypeEnum.PRODUCT.name().equals(scope.getScopeType())) {
                hasItemScope = true;
                if (scope.getTenantId() == null || Objects.equals(scope.getTenantId(), tenantId)) {
                    if (scope.getScopeId() != null) {
                        productScopeIds.add(scope.getScopeId());
                    }
                }
                if (scope.getTenantId() != null) {
                    scopedTenantIds.add(scope.getTenantId());
                }
            } else if (CouponScopeTypeEnum.CATEGORY.name().equals(scope.getScopeType())) {
                hasItemScope = true;
                if (scope.getTenantId() == null || Objects.equals(scope.getTenantId(), tenantId)) {
                    if (scope.getScopeCode() != null) {
                        categoryScopeCodes.add(scope.getScopeCode());
                    }
                }
                if (scope.getTenantId() != null) {
                    scopedTenantIds.add(scope.getTenantId());
                }
            }
        }
        if ((hasTenantScope && !tenantMatched) || (hasUserScope && !userMatched)) {
            return BigDecimal.ZERO;
        }
        if (!scopedTenantIds.isEmpty() && !scopedTenantIds.contains(tenantId)) {
            return BigDecimal.ZERO;
        }
        return hasItemScope ? sumScopedItems(items, productScopeIds, categoryScopeCodes) : orderAmount;
    }

    private List<CouponScope> listScopes(Long couponTemplateId) {
        List<CouponScope> scopes = couponScopeMapper.selectList(new LambdaQueryWrapper<CouponScope>()
                .eq(CouponScope::getCouponTemplateId, couponTemplateId)
                .eq(CouponScope::getDeleted, 0));
        return scopes == null ? Collections.emptyList() : scopes;
    }

    /** 批量加载多个模板的 scope，消除 N+1 查询 */
    private Map<Long, List<CouponScope>> batchLoadScopes(Set<Long> templateIds) {
        if (templateIds == null || templateIds.isEmpty()) {
            return Collections.emptyMap();
        }
        List<CouponScope> allScopes = couponScopeMapper.selectList(new LambdaQueryWrapper<CouponScope>()
                .in(CouponScope::getCouponTemplateId, templateIds)
                .eq(CouponScope::getDeleted, 0));
        if (allScopes == null || allScopes.isEmpty()) {
            return Collections.emptyMap();
        }
        return allScopes.stream()
                .collect(Collectors.groupingBy(CouponScope::getCouponTemplateId));
    }

    private BigDecimal sumItems(List<OrderPricingItemDTO> items) {
        if (items == null || items.isEmpty()) {
            return BigDecimal.ZERO;
        }
        return items.stream()
                .map(item -> safeAmount(item.getUnitPrice()).multiply(BigDecimal.valueOf(defaultQuantity(item))))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private BigDecimal sumScopedItems(List<OrderPricingItemDTO> items, Set<Long> productIds, Set<String> categories) {
        if (items == null || items.isEmpty()) {
            return BigDecimal.ZERO;
        }
        return items.stream()
                .filter(item -> productIds.contains(item.getProductId()) || categories.contains(item.getCategory()))
                .map(item -> safeAmount(item.getUnitPrice()).multiply(BigDecimal.valueOf(defaultQuantity(item))))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private BigDecimal safeAmount(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private int defaultQuantity(OrderPricingItemDTO item) {
        return item.getQuantity() == null || item.getQuantity() <= 0 ? 0 : item.getQuantity();
    }
}
