package com.payment.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.payment.dto.AppCreateOrderDTO;
import com.payment.dto.OrderPaymentVO;
import com.payment.entity.SalesOrder;

public interface AppOrderService {
    OrderPaymentVO createOrder(Long platformUserId, AppCreateOrderDTO dto);

    Page<SalesOrder> listOrders(Long platformUserId, Integer current, Integer size);

    SalesOrder getByOrderNo(Long platformUserId, String orderNo);

    void cancelOrder(Long platformUserId, String orderNo);
}
