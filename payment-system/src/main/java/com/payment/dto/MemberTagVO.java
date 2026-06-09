package com.payment.dto;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 会员标签视图对象（V1 MerchantMarketing 接口）
 */
@Data
public class MemberTagVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;
    private Long tenantId;
    private String tagCode;
    private String tagName;
    private String tagType;
    private String tagColor;
    private String description;
    private Integer status;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
