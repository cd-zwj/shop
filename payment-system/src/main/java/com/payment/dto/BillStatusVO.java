package com.payment.dto;

import lombok.Data;

/**
 * 账单状态查询 VO（对外暴露的安全视图，仅含 billNo 和 payStatus）。
 */
@Data
public class BillStatusVO {
    private String billNo;
    private String payStatus;
}
