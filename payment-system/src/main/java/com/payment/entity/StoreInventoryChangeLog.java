package com.payment.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 门店库存流水。记录补货、调整、锁定、释放和销售扣减，禁止更新或删除。
 */
@Data
@TableName("store_inventory_change_log")
public class StoreInventoryChangeLog implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long tenantId;
    private Long storeId;
    private Long productId;
    private String changeType;
    private Integer changeQuantity;
    private Integer quantityBefore;
    private Integer quantityAfter;
    private Integer lockedBefore;
    private Integer lockedAfter;
    private String bizType;
    private String bizNo;
    private Long operatorId;
    private String remark;
    private LocalDateTime createTime;
}
