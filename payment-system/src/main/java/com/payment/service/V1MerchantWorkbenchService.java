package com.payment.service;

import com.payment.common.PageResult;
import com.payment.dto.MerchantWorkbenchTaskVO;
import com.payment.dto.MerchantWorkbenchTodoSummaryVO;

/**
 * 商户工作台聚合服务。
 */
public interface V1MerchantWorkbenchService {

    MerchantWorkbenchTodoSummaryVO getTodoSummary(Long tenantId);

    PageResult<MerchantWorkbenchTaskVO> listVisibleTasks(Long tenantId, String type, Integer pageNum, Integer pageSize);
}
