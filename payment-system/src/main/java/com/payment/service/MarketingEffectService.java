package com.payment.service;

import com.payment.dto.CouponEffectVO;
import com.payment.dto.MarketingEffectSummaryVO;

public interface MarketingEffectService {
    MarketingEffectSummaryVO getSummary(Long tenantId, Long platformUserId);

    CouponEffectVO getCouponEffect(Long tenantId, Long platformUserId, Long templateId);
}
