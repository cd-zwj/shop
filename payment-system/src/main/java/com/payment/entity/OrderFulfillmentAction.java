package com.payment.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/** 门店自提订单的不可变履约操作流水。 */
@Data
@TableName("order_fulfillment_action")
public class OrderFulfillmentAction implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long tenantId;
    private Long storeId;
    private Long orderId;
    private String orderNo;
    private String action;
    private String fromStatus;
    private String toStatus;
    private Long operatorId;
    private String remark;
    private LocalDateTime createTime;
}
