package com.payment.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * Recovered平台账号视图对象，用于返回Recovered平台账号展示数据。
 */
@Data
@AllArgsConstructor
public class RecoveredPlatformAccountVO {

    private String username;
}
