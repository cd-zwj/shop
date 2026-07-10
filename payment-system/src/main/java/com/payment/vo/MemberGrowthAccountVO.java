package com.payment.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * 用户端成长值概览视图对象。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MemberGrowthAccountVO {

    /** 当前成长值总额 */
    private Integer totalGrowth;
    /** 当前等级ID */
    private Long levelId;
    /** 当前等级名称 */
    private String levelName;
    /** 下一等级升级所需成长值，null 表示已达最高等级 */
    private Integer nextLevelGrowth;
    /** 当前等级折扣率，如 0.95 表示九五折 */
    private BigDecimal discountRate;
    /** 当前等级权益 JSON */
    private String benefitJson;
}
