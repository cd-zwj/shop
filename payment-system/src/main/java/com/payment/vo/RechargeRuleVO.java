package com.payment.vo;

import com.payment.entity.MerchantRechargeRule;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * 用户端商户充值规则视图对象，隐藏 tenantId、status 等内部字段。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RechargeRuleVO {

    private Long id;
    private Long rechargeAmount;
    private Long giftAmount;
    private Integer giftPoints;
    private Integer sortOrder;

    public static RechargeRuleVO from(MerchantRechargeRule rule) {
        if (rule == null) {
            return null;
        }
        return RechargeRuleVO.builder()
                .id(rule.getId())
                .rechargeAmount(toFen(rule.getRechargeAmount()))
                .giftAmount(toFen(rule.getGiftAmount()))
                .giftPoints(rule.getGiftPoints())
                .sortOrder(rule.getSortOrder())
                .build();
    }

    private static Long toFen(BigDecimal amount) {
        return amount == null ? null : amount.multiply(new BigDecimal(100)).longValue();
    }
}
