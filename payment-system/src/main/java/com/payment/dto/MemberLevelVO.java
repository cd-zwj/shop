package com.payment.dto;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 会员等级视图对象，用于返回商户会员等级的配置信息（V1 MerchantMarketing 接口）。
 */
@Data
public class MemberLevelVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 等级 ID */
    private Long id;
    /** 所属商户租户 ID */
    private Long tenantId;
    /** 等级编号（业务唯一标识） */
    private String levelNo;
    /** 等级名称（如 普通会员、银卡、金卡） */
    private String levelName;
    /** 等级排序（值越大等级越高） */
    private Integer levelRank;
    /** 升级所需成长值 */
    private Integer upgradeGrowth;
    /** 会员折扣率（如 0.95 表示 95 折） */
    private BigDecimal discountRate;
    /** 会员权益 JSON（存放额外权益配置） */
    private String benefitJson;
    /** 状态（0-禁用, 1-启用） */
    private Integer status;
    /** 创建时间 */
    private LocalDateTime createTime;
    /** 更新时间 */
    private LocalDateTime updateTime;
}
