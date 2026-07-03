package com.payment.dto;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 商户端卡密明细视图对象，展示单条卡密的完整生命周期信息。
 */
@Data
public class V1MerchantCardKeyVO {

    /** 卡密ID */
    private Long id;

    /** 所属租户（商户）ID */
    private Long tenantId;

    /** 关联商品ID */
    private Long productId;

    /** 卡密编码 */
    private String cardCode;

    /** 卡密状态（AVAILABLE / USED / RETURNED / DISABLED） */
    private String status;

    /** 关联的订单编号（使用后填充） */
    private String orderNo;

    /** 关联的订单项ID（使用后填充） */
    private Long orderItemId;

    /** 使用时间 */
    private LocalDateTime usedTime;

    /** 退回时间 */
    private LocalDateTime returnedTime;

    /** 退回原因 */
    private String returnReason;

    /** 创建时间 */
    private LocalDateTime createTime;

    /** 最后更新时间 */
    private LocalDateTime updateTime;
}
