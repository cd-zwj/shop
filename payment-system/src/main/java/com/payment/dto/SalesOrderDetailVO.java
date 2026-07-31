package com.payment.dto;

import com.payment.entity.SalesOrder;
import com.payment.entity.SalesOrderItem;
import lombok.Data;

import java.util.List;
import java.util.Map;
import java.time.LocalDateTime;

/**
 * 订单详情视图对象，用于返回订单完整信息（含商品项和支付账单号）。
 */
@Data
public class SalesOrderDetailVO {

    /** 订单主体信息 */
    private SalesOrder order;

    /** 订单商品项列表 */
    private List<SalesOrderItem> items;

    /** 仅 C 端授权详情填充；key 为订单项 ID。 */
    private Map<Long, String> pickupCodesByOrderItemId;

    /** 关联的支付账单编号 */
    private String paymentBillNo;

    /** 最近一笔关联支付单状态 */
    private String paymentBillStatus;

    /** 最近一笔关联支付单状态说明 */
    private String paymentBillStatusRemark;

    /** 最近一笔关联支付单过期时间 */
    private LocalDateTime paymentBillExpireTime;
}
