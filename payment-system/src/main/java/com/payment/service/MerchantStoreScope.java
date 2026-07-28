package com.payment.service;

import java.util.List;

/** 已通过商户身份与模块权限校验的门店数据范围。 */
public record MerchantStoreScope(Long tenantId, Long employeeId, boolean allStores, List<Long> storeIds) {

    public MerchantStoreScope {
        storeIds = storeIds == null ? List.of() : List.copyOf(storeIds);
    }
}
