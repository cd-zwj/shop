package com.payment.dto;

import com.payment.entity.SalesOrder;
import com.payment.entity.SalesOrderItem;
import lombok.Data;

import java.util.List;

/**
 * 订单详情视图对象，用于返回订单完整信息（含商品项和支付账单号）。
 */
@Data
public class SalesOrderDetailVO {

    /** 订单主体信息 */
    private SalesOrder order;

    /** 订单商品项列表 */
    private List<SalesOrderItem> items;

    /** 关联的支付账单编号 */
    private String paymentBillNo;
}
