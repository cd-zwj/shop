package com.payment.dto;

import com.payment.entity.CouponTemplate;
import com.payment.entity.MemberGrowthLog;
import com.payment.entity.MemberPointsLog;
import com.payment.entity.UserCoupon;
import com.payment.enums.UserCouponStatusEnum;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class AssetTracePresentationsTest {

    @Test
    void walletTraceLinksOrderPaymentBackToOrderDetail() {
        WalletLogVO log = new WalletLogVO();
        log.setWalletType("UNIFIED");
        log.setBizType("SALES_ORDER");
        log.setBizNo("SO202607070001");
        log.setChangeAmount(new BigDecimal("-80.00"));
        log.setBalanceBefore(new BigDecimal("100.00"));
        log.setBalanceAfter(new BigDecimal("20.00"));
        log.setRemark("订单支付");

        AssetTracePresentation trace = AssetTracePresentations.wallet(log);

        assertThat(trace.getTitle()).isEqualTo("订单支付");
        assertThat(trace.getSource()).contains("统一钱包", "SO202607070001");
        assertThat(trace.getActionLabel()).isEqualTo("查看订单");
        assertThat(trace.getActionPath()).isEqualTo("/order/SO202607070001");
        assertThat(trace.getTone()).isEqualTo("negative");
    }

    @Test
    void pointsTraceUsesOrderSourceAndBalanceTransition() {
        MemberPointsLog log = new MemberPointsLog();
        log.setBizType("ORDER_REWARD");
        log.setBizNo("SO202607070002");
        log.setChangePoints(120);
        log.setPointsBefore(200);
        log.setPointsAfter(320);
        log.setStatus("CONFIRMED");
        log.setExpireTime(LocalDateTime.of(2026, 8, 7, 23, 59));
        log.setRemark("订单返积分");

        AssetTracePresentation trace = AssetTracePresentations.points(log);

        assertThat(trace.getTitle()).isEqualTo("订单返积分");
        assertThat(trace.getSource()).contains("订单返积分", "SO202607070002");
        assertThat(trace.getEffect()).isEqualTo("+120 积分");
        assertThat(trace.getBalance()).isEqualTo("200 -> 320");
        assertThat(trace.getActionPath()).isEqualTo("/order/SO202607070002");
    }

    @Test
    void growthTraceLinksOrderGrowthBackToOrderDetail() {
        MemberGrowthLog log = new MemberGrowthLog();
        log.setBizType("ORDER");
        log.setBizNo("SO202607070003");
        log.setChangeType("EARN");
        log.setChangeGrowth(35);
        log.setGrowthBefore(100);
        log.setGrowthAfter(135);

        AssetTracePresentation trace = AssetTracePresentations.growth(log);

        assertThat(trace.getTitle()).isEqualTo("订单消费");
        assertThat(trace.getEffect()).isEqualTo("+35 成长值");
        assertThat(trace.getBalance()).isEqualTo("100 -> 135");
        assertThat(trace.getActionLabel()).isEqualTo("查看订单");
    }

    @Test
    void couponTraceShowsUsedOrderAndAction() {
        UserCoupon coupon = new UserCoupon();
        coupon.setTenantId(9L);
        coupon.setCouponStatus(UserCouponStatusEnum.USED.name());
        coupon.setOrderNo("SO202607070004");
        coupon.setReceiveTime(LocalDateTime.of(2026, 7, 1, 10, 0));
        coupon.setExpireTime(LocalDateTime.of(2026, 7, 31, 23, 59));
        coupon.setUseTime(LocalDateTime.of(2026, 7, 7, 11, 0));
        CouponTemplate template = new CouponTemplate();
        template.setTemplateName("满减券");

        AssetTracePresentation trace = AssetTracePresentations.coupon(coupon, template);

        assertThat(trace.getTitle()).isEqualTo("满减券");
        assertThat(trace.getSource()).isEqualTo("使用订单 SO202607070004");
        assertThat(trace.getStatus()).isEqualTo("已使用");
        assertThat(trace.getActionPath()).isEqualTo("/order/SO202607070004");
    }
}
