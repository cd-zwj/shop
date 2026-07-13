package com.payment.dto;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 用户端统一资产动态。
 */
@Data
public class AppAssetActivityVO {
    private String assetType;
    private String title;
    private String description;
    private LocalDateTime occurredAt;
    private Long tenantId;
    private String tenantName;
    private String bizNo;
    private String bizType;
    private String amountText;
    private String tone;
    private String actionPath;
    private String sourceType;
    private Long sourceId;
}
