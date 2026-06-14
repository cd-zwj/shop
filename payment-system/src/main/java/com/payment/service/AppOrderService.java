package com.payment.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.payment.dto.AppCreateOrderDTO;
import com.payment.dto.OrderPaymentVO;
import com.payment.dto.SalesOrderDetailVO;
import com.payment.entity.SalesOrder;
import com.payment.entity.SalesOrderItem;
import com.payment.enums.PaymentChannelCodeEnum;

import java.util.List;

public interface AppOrderService {
    OrderPaymentVO createOrder(Long platformUserId, AppCreateOrderDTO dto);

    Page<SalesOrder> listOrders(Long platformUserId, Integer current, Integer size);

    SalesOrder getByOrderNo(Long platformUserId, String orderNo);

    SalesOrderDetailVO getOrderDetail(Long platformUserId, String orderNo);

    SalesOrderDetailVO getMerchantOrderDetail(Long tenantId, Long platformUserId, String orderNo);

    Page<SalesOrder> listMerchantOrders(Long tenantId, Integer current, Integer size, String orderStatus, String payStatus, String keyword);

    List<SalesOrderItem> listOrderItems(Long platformUserId, String orderNo);

    OrderPaymentVO repayOrder(Long platformUserId, String orderNo, PaymentChannelCodeEnum paymentChannelCode);

    void cancelOrder(Long platformUserId, String orderNo);
    /**
     * 处理支付回调重试：根据 PaymentBill 的状态更新关联 SalesOrder。
     *
     * @param paymentBillNo 支付单号
     */
    void handlePaymentCallback(String paymentBillNo);
}