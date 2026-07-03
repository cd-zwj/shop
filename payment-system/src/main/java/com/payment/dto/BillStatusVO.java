package com.payment.dto;

import lombok.Data;

/**
 * 账单状态查询视图对象，用于对外暴露安全的支付账单状态信息（仅含账单号和支付状态）。
 */
@Data
public class BillStatusVO {
    /** 账单编号 */
    private String billNo;
    /** 支付状态（如 UNPAID, PAID, REFUNDED） */
    private String payStatus;
}
