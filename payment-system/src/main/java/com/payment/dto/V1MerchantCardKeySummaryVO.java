package com.payment.dto;

import lombok.Data;

/**
 * 商户端卡密库存汇总视图对象，按商品维度统计各状态的卡密数量。
 */
@Data
public class V1MerchantCardKeySummaryVO {

    /** 商品ID */
    private Long productId;

    /** 可用卡密数量 */
    private Integer availableCount;

    /** 已使用卡密数量 */
    private Integer usedCount;

    /** 已退回卡密数量 */
    private Integer returnedCount;

    /** 已禁用卡密数量 */
    private Integer disabledCount;

    /** 卡密总数 */
    private Integer totalCount;
}
