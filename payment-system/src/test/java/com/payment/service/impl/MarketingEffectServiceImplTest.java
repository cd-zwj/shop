package com.payment.service.impl;

import com.payment.constant.MerchantPermission;
import com.payment.dto.MarketingEffectSummaryVO;
import com.payment.entity.CouponTemplate;
import com.payment.entity.OrderDiscountSnapshot;
import com.payment.entity.PromotionActivity;
import com.payment.enums.CouponOwnerTypeEnum;
import com.payment.mapper.CouponTemplateMapper;
import com.payment.mapper.OrderDiscountSnapshotMapper;
import com.payment.mapper.PromotionActivityMapper;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class MarketingEffectServiceImplTest {

    @Test
    void getSummaryShouldIncludeActivityEffectMetrics() {
        CouponTemplateMapper couponTemplateMapper = mock(CouponTemplateMapper.class);
        PromotionActivityMapper promotionActivityMapper = mock(PromotionActivityMapper.class);
        OrderDiscountSnapshotMapper discountSnapshotMapper = mock(OrderDiscountSnapshotMapper.class);
        V1MerchantSupportService supportService = mock(V1MerchantSupportService.class);
        MarketingEffectServiceImpl service = new MarketingEffectServiceImpl(
                couponTemplateMapper,
                promotionActivityMapper,
                discountSnapshotMapper,
                supportService
        );

        CouponTemplate coupon = new CouponTemplate();
        coupon.setTemplateScope(CouponOwnerTypeEnum.TENANT.name());
        coupon.setStatus("ACTIVE");
        coupon.setReceivedQuantity(10);
        coupon.setUsedQuantity(4);
        coupon.setTotalQuantity(20);
        PromotionActivity draftActivity = new PromotionActivity();
        draftActivity.setStatus("DRAFT");
        PromotionActivity activeActivity = new PromotionActivity();
        activeActivity.setStatus("ACTIVE");
        OrderDiscountSnapshot activityDiscount = new OrderDiscountSnapshot();
        activityDiscount.setDiscountAmount(new BigDecimal("12.50"));

        when(couponTemplateMapper.selectList(any())).thenReturn(List.of(coupon));
        when(promotionActivityMapper.selectList(any())).thenReturn(List.of(draftActivity, activeActivity));
        when(discountSnapshotMapper.selectList(any())).thenReturn(List.of(activityDiscount));

        MarketingEffectSummaryVO result = service.getSummary(9L, 100L);

        verify(supportService).requirePermission(9L, 100L, MerchantPermission.MARKETING_MANAGE);
        assertEquals(2, result.getActivityCount());
        assertEquals(1, result.getActiveActivityCount());
        assertEquals(new BigDecimal("12.50"), result.getActivityDiscountAmount());
    }
}
