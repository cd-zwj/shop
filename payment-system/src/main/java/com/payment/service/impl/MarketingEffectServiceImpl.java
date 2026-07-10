package com.payment.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.payment.common.BusinessException;
import com.payment.constant.MerchantPermission;
import com.payment.dto.CouponEffectVO;
import com.payment.dto.MarketingEffectSummaryVO;
import com.payment.entity.CouponTemplate;
import com.payment.entity.OrderDiscountSnapshot;
import com.payment.entity.PromotionActivity;
import com.payment.enums.CouponOwnerTypeEnum;
import com.payment.mapper.CouponTemplateMapper;
import com.payment.mapper.OrderDiscountSnapshotMapper;
import com.payment.mapper.PromotionActivityMapper;
import com.payment.service.MarketingEffectService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class MarketingEffectServiceImpl implements MarketingEffectService {

    private final CouponTemplateMapper couponTemplateMapper;
    private final PromotionActivityMapper promotionActivityMapper;
    private final OrderDiscountSnapshotMapper orderDiscountSnapshotMapper;
    private final V1MerchantSupportService v1MerchantSupportService;

    @Override
    public MarketingEffectSummaryVO getSummary(Long tenantId, Long platformUserId) {
        v1MerchantSupportService.requirePermission(tenantId, platformUserId, MerchantPermission.MARKETING_MANAGE);
        List<CouponTemplate> templates = couponTemplateMapper.selectList(baseTenantCouponQuery(tenantId));
        List<PromotionActivity> activities = promotionActivityMapper.selectList(new LambdaQueryWrapper<PromotionActivity>()
                .eq(PromotionActivity::getTenantId, tenantId)
                .eq(PromotionActivity::getActivityScope, CouponOwnerTypeEnum.TENANT.name())
                .eq(PromotionActivity::getDeleted, 0));
        List<OrderDiscountSnapshot> activityDiscounts = orderDiscountSnapshotMapper.selectList(new LambdaQueryWrapper<OrderDiscountSnapshot>()
                .eq(OrderDiscountSnapshot::getTenantId, tenantId)
                .eq(OrderDiscountSnapshot::getDiscountSource, "ACTIVITY"));

        int receivedCount = templates.stream().mapToInt(t -> value(t.getReceivedQuantity())).sum();
        int usedCount = templates.stream().mapToInt(t -> value(t.getUsedQuantity())).sum();
        int remainingStock = templates.stream().mapToInt(this::remainingStock).sum();
        BigDecimal activityDiscountAmount = activityDiscounts.stream()
                .map(OrderDiscountSnapshot::getDiscountAmount)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        MarketingEffectSummaryVO vo = new MarketingEffectSummaryVO();
        vo.setTemplateCount(templates.size());
        vo.setActiveTemplateCount((int) templates.stream().filter(t -> "ACTIVE".equals(t.getStatus())).count());
        vo.setReceivedCount(receivedCount);
        vo.setUsedCount(usedCount);
        vo.setRemainingStock(remainingStock);
        vo.setWriteOffRate(rate(usedCount, receivedCount));
        vo.setActivityCount(activities.size());
        vo.setActiveActivityCount((int) activities.stream().filter(a -> "ACTIVE".equals(a.getStatus())).count());
        vo.setActivityDiscountAmount(activityDiscountAmount);
        return vo;
    }

    @Override
    public CouponEffectVO getCouponEffect(Long tenantId, Long platformUserId, Long templateId) {
        v1MerchantSupportService.requirePermission(tenantId, platformUserId, MerchantPermission.MARKETING_MANAGE);
        CouponTemplate template = couponTemplateMapper.selectOne(baseTenantCouponQuery(tenantId)
                .eq(CouponTemplate::getId, templateId));
        if (template == null) {
            throw new BusinessException("优惠券模板不存在");
        }

        CouponEffectVO vo = new CouponEffectVO();
        vo.setTemplateId(template.getId());
        vo.setTemplateName(template.getTemplateName());
        vo.setTotalQuantity(template.getTotalQuantity());
        vo.setReceivedCount(value(template.getReceivedQuantity()));
        vo.setUsedCount(value(template.getUsedQuantity()));
        vo.setRemainingStock(remainingStock(template));
        vo.setWriteOffRate(rate(vo.getUsedCount(), vo.getReceivedCount()));
        return vo;
    }

    private LambdaQueryWrapper<CouponTemplate> baseTenantCouponQuery(Long tenantId) {
        return new LambdaQueryWrapper<CouponTemplate>()
                .eq(CouponTemplate::getTenantId, tenantId)
                .eq(CouponTemplate::getTemplateScope, CouponOwnerTypeEnum.TENANT.name())
                .eq(CouponTemplate::getDeleted, 0);
    }

    private int remainingStock(CouponTemplate template) {
        Integer total = template.getTotalQuantity();
        if (total == null || total <= 0) {
            return -1;
        }
        return Math.max(total - value(template.getReceivedQuantity()), 0);
    }

    private int value(Integer number) {
        return Objects.requireNonNullElse(number, 0);
    }

    private double rate(int used, int received) {
        if (received <= 0) {
            return 0D;
        }
        return Math.round((used * 10000D / received)) / 10000D;
    }
}
