package com.payment.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * 找回账号结果视图对象，用于返回通过邮箱找回的平台账号信息。
 */
@Data
@AllArgsConstructor
public class RecoveredPlatformAccountVO {

    /** 找回的用户名（脱敏后） */
    private String username;
}
