package com.payment.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.payment.dto.CardKeyDeliveryDTO;
import com.payment.dto.V1MerchantCardKeySummaryVO;
import com.payment.dto.V1MerchantCardKeyUploadDTO;
import com.payment.dto.V1MerchantCardKeyVO;

public interface CardKeyPoolService {

    Page<V1MerchantCardKeyVO> listMerchantCardKeys(Long tenantId, Long platformUserId, Long productId,
                                                   Integer current, Integer size, String status);

    V1MerchantCardKeySummaryVO getMerchantSummary(Long tenantId, Long platformUserId, Long productId);

    V1MerchantCardKeySummaryVO uploadMerchantCardKeys(Long tenantId, Long platformUserId, Long productId,
                                                      V1MerchantCardKeyUploadDTO dto);

    CardKeyDeliveryDTO lockForDelivery(Long tenantId, Long productId, String orderNo, Long orderItemId);

    void returnByOrderItem(Long tenantId, Long orderItemId, String reason);

    void returnByCardKeyId(Long tenantId, Long cardKeyId, String reason);
}
