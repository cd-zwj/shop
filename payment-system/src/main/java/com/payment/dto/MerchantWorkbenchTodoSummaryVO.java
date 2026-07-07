package com.payment.dto;

import lombok.Builder;
import lombok.Value;

import java.io.Serializable;
import java.util.List;

/**
 * 商户工作台待办汇总。
 */
@Value
@Builder
public class MerchantWorkbenchTodoSummaryVO implements Serializable {
    Long totalCount;
    List<MerchantWorkbenchTodoItemVO> items;
}
