package com.payment.service;

import com.payment.dto.MerchantWorkbenchTodoSummaryVO;

/**
 * 商户工作台聚合服务。
 */
public interface V1MerchantWorkbenchService {

    MerchantWorkbenchTodoSummaryVO getTodoSummary(Long tenantId);
}
