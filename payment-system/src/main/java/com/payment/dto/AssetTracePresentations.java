package com.payment.dto;

import com.payment.entity.CouponTemplate;
import com.payment.entity.MemberGrowthLog;
import com.payment.entity.MemberPointsLog;
import com.payment.entity.UserCoupon;
import com.payment.enums.UserCouponStatusEnum;

import java.math.BigDecimal;

/**
 * 用户资产明细展示字段生成器。
 */
public final class AssetTracePresentations {

    private AssetTracePresentations() {
    }

    public static AssetTracePresentation wallet(WalletLogVO log) {
        BigDecimal amount = defaultAmount(log.getChangeAmount());
        String bizType = log.getBizType();
        String bizNo = trimToNull(log.getBizNo());
        String tone = amount.compareTo(BigDecimal.ZERO) > 0 ? "positive"
                : amount.compareTo(BigDecimal.ZERO) < 0 ? "negative" : "neutral";

        AssetTracePresentation.AssetTracePresentationBuilder builder = AssetTracePresentation.builder()
                .title(walletBizLabel(bizType))
                .source(walletSource(log))
                .effect((amount.compareTo(BigDecimal.ZERO) > 0 ? "+" : "") + amount.stripTrailingZeros().toPlainString())
                .balance(formatAmount(log.getBalanceBefore()) + " -> " + formatAmount(log.getBalanceAfter()))
                .tone(tone);

        if (bizNo != null) {
            if ("SALES_ORDER".equals(bizType) || "ORDER_CANCEL_REFUND".equals(bizType)) {
                builder.actionLabel("查看订单").actionPath(orderPath(bizNo));
            } else if ("UNIFIED_RECHARGE".equals(bizType) || "MERCHANT_RECHARGE".equals(bizType) || "RECHARGE".equals(bizType)) {
                builder.actionLabel("查看支付状态").actionPath("/payment/status?bizNo=" + encodePath(bizNo) + "&source=recharge");
            } else if ("MERCHANT_APPROVED_REFUND".equals(bizType) || "LATE_CALLBACK_REFUND".equals(bizType) || "REFUND".equals(bizType)) {
                builder.actionLabel("查看售后").actionPath("/orders");
            }
        }

        return builder.build();
    }

    public static AssetTracePresentation points(MemberPointsLog log) {
        int change = defaultInt(log.getChangePoints());
        String bizNo = trimToNull(log.getBizNo());
        String title = trimToNull(log.getRemark()) != null ? log.getRemark() : pointsBizLabel(log.getBizType(), change);
        AssetTracePresentation.AssetTracePresentationBuilder builder = AssetTracePresentation.builder()
                .title(title)
                .source(bizNo == null ? "来源：" + pointsBizLabel(log.getBizType(), change) : "来源：" + pointsBizLabel(log.getBizType(), change) + " " + bizNo)
                .effect((change > 0 ? "+" : "") + change + " 积分")
                .balance(defaultInt(log.getPointsBefore()) + " -> " + defaultInt(log.getPointsAfter()))
                .status(log.getStatus())
                .hint(log.getExpireTime() == null ? null : "将于 " + log.getExpireTime() + " 过期")
                .tone(change > 0 ? "positive" : change < 0 ? "negative" : "neutral");
        if (isOrderBiz(log.getBizType()) && bizNo != null) {
            builder.actionLabel("查看订单").actionPath(orderPath(bizNo));
        }
        return builder.build();
    }

    public static AssetTracePresentation growth(MemberGrowthLog log) {
        int change = defaultInt(log.getChangeGrowth());
        String bizNo = trimToNull(log.getBizNo());
        String label = growthBizLabel(log.getBizType());
        AssetTracePresentation.AssetTracePresentationBuilder builder = AssetTracePresentation.builder()
                .title(trimToNull(log.getRemark()) != null ? log.getRemark() : label)
                .source(bizNo == null ? "来源：" + label : "来源：" + label + " " + bizNo)
                .effect((change > 0 ? "+" : "") + change + " 成长值")
                .balance(defaultInt(log.getGrowthBefore()) + " -> " + defaultInt(log.getGrowthAfter()))
                .status(log.getChangeType())
                .tone(change > 0 ? "positive" : change < 0 ? "negative" : "neutral");
        if ("ORDER".equals(log.getBizType()) && bizNo != null) {
            builder.actionLabel("查看订单").actionPath(orderPath(bizNo));
        }
        return builder.build();
    }

    public static AssetTracePresentation coupon(UserCoupon coupon, CouponTemplate template) {
        String status = coupon.getCouponStatus();
        String orderNo = trimToNull(coupon.getOrderNo());
        AssetTracePresentation.AssetTracePresentationBuilder builder = AssetTracePresentation.builder()
                .title(template == null ? "优惠券" : template.getTemplateName())
                .source("领取时间 " + coupon.getReceiveTime())
                .hint("有效期至 " + coupon.getExpireTime());

        if (UserCouponStatusEnum.USED.name().equals(status)) {
            builder.status("已使用")
                    .source(orderNo == null ? "领取时间 " + coupon.getReceiveTime() : "使用订单 " + orderNo)
                    .hint("使用时间 " + coupon.getUseTime())
                    .tone("neutral");
            if (orderNo != null) {
                builder.actionLabel("查看订单").actionPath(orderPath(orderNo));
            } else {
                builder.inactiveActionLabel("已使用");
            }
            return builder.build();
        }

        if (UserCouponStatusEnum.EXPIRED.name().equals(status)) {
            return builder.status("已过期")
                    .inactiveActionLabel("已过期")
                    .tone("negative")
                    .build();
        }

        return builder.status("可使用")
                .actionLabel("去使用")
                .actionPath(coupon.getTenantId() == null ? "/" : "/?tenantId=" + coupon.getTenantId())
                .tone("positive")
                .build();
    }

    private static String walletSource(WalletLogVO log) {
        String wallet = "MERCHANT".equals(log.getWalletType()) && log.getTenantId() != null
                ? "商户 #" + log.getTenantId()
                : "UNIFIED".equals(log.getWalletType()) ? "统一钱包" : log.getWalletType();
        String bizNo = trimToNull(log.getBizNo());
        String remark = trimToNull(log.getRemark());
        StringBuilder source = new StringBuilder(wallet == null ? "钱包流水" : wallet);
        if (bizNo != null) {
            source.append(" · 业务号 ").append(bizNo);
        }
        if (remark != null) {
            source.append(" · ").append(remark);
        }
        return source.toString();
    }

    private static String walletBizLabel(String bizType) {
        if ("SALES_ORDER".equals(bizType)) return "订单支付";
        if ("UNIFIED_RECHARGE".equals(bizType)) return "统一钱包充值";
        if ("MERCHANT_RECHARGE".equals(bizType)) return "商户钱包充值";
        if ("ORDER_CANCEL_REFUND".equals(bizType)) return "订单取消退款";
        if ("MERCHANT_APPROVED_REFUND".equals(bizType)) return "售后退款";
        if ("LATE_CALLBACK_REFUND".equals(bizType)) return "异常支付退款";
        if ("REFUND".equals(bizType)) return "退款";
        if (bizType == null || bizType.isBlank()) return "钱包流水";
        return bizType.replace('_', ' ');
    }

    private static String pointsBizLabel(String bizType, int change) {
        if ("ORDER_REWARD".equals(bizType)) return "订单返积分";
        if ("ORDER_DEDUCT".equals(bizType)) return "订单抵扣";
        if ("MERCHANT_RECHARGE".equals(bizType)) return "充值赠送";
        if ("POINTS_EXPIRE".equals(bizType)) return "积分过期";
        if (bizType == null || bizType.isBlank()) return change >= 0 ? "积分入账" : "积分支出";
        return bizType.replace('_', ' ');
    }

    private static String growthBizLabel(String bizType) {
        if ("ORDER".equals(bizType)) return "订单消费";
        if ("RECHARGE".equals(bizType)) return "充值";
        if ("MANUAL".equals(bizType)) return "人工调整";
        if (bizType == null || bizType.isBlank()) return "成长值变动";
        return bizType.replace('_', ' ');
    }

    private static boolean isOrderBiz(String bizType) {
        return bizType != null && bizType.startsWith("ORDER");
    }

    private static String orderPath(String orderNo) {
        return "/order/" + encodePath(orderNo);
    }

    private static String encodePath(String value) {
        return value.replace(" ", "%20");
    }

    private static BigDecimal defaultAmount(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private static int defaultInt(Integer value) {
        return value == null ? 0 : value;
    }

    private static String formatAmount(BigDecimal amount) {
        return defaultAmount(amount).stripTrailingZeros().toPlainString();
    }

    private static String trimToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}
