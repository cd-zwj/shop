package com.payment.dto;

import com.payment.entity.SalesOrder;
import com.payment.entity.SalesOrderItem;
import lombok.Data;

import java.util.List;

/**
 * 订单详情视图对象。
 */
@Data
public class SalesOrderDetailVO {

    private SalesOrder order;

    private List<SalesOrderItem> items;
}
