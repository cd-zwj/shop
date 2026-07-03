package com.payment.dto;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 会员标签视图对象，用于返回商户自定义的会员标签信息（V1 MerchantMarketing 接口）。
 */
@Data
public class MemberTagVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 标签 ID */
    private Long id;
    /** 所属商户租户 ID */
    private Long tenantId;
    /** 标签编码（如 VIP, NEW_USER, HIGH_VALUE） */
    private String tagCode;
    /** 标签名称（如 重要客户、新用户、高消费） */
    private String tagName;
    /** 标签类型（如 SYSTEM-系统标签、CUSTOM-自定义标签） */
    private String tagType;
    /** 标签颜色（十六进制色值，用于前端展示） */
    private String tagColor;
    /** 标签描述 */
    private String description;
    /** 状态（0-禁用, 1-启用） */
    private Integer status;
    /** 创建时间 */
    private LocalDateTime createTime;
    /** 更新时间 */
    private LocalDateTime updateTime;
}
