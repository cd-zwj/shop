package com.payment.dto;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 用户受限资产明细。
 */
@Data
public class AssetHoldVO {
    private Long tenantId;
    private String tenantName;
    private String assetType;
    private String holdStatus;
    private String amountText;
    private String reason;
    private String bizType;
    private String bizNo;
    private LocalDateTime occurredAt;
    private String actionPath;
    private String actionLabel;
}
