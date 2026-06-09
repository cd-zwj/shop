package com.payment.dto;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 会员等级视图对象（V1 MerchantMarketing 接口）
 */
@Data
public class MemberLevelVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;
    private Long tenantId;
    private String levelNo;
    private String levelName;
    private Integer levelRank;
    private Integer upgradeGrowth;
    private BigDecimal discountRate;
    private String benefitJson;
    private Integer status;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
