package com.payment.service;

import com.payment.entity.OrderFulfillmentAction;

import java.util.List;

/** 门店自提备货状态机。 */
public interface OrderFulfillmentService {

    void startPreparation(Long tenantId, String orderNo, Long operatorId, String remark);

    void completePreparation(Long tenantId, String orderNo, Long operatorId, String remark);

    List<OrderFulfillmentAction> listActions(Long tenantId, String orderNo);
}
