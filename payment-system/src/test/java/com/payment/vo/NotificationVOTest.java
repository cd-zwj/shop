package com.payment.vo;

import com.payment.entity.UserNotification;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class NotificationVOTest {

    @Test
    void orderNotificationShouldExposeOrderDetailAction() {
        UserNotification notification = notification("ORDER", "您的订单 SO202607050001 已支付成功");

        NotificationVO vo = NotificationVO.from(notification);

        assertThat(vo.getActionType()).isEqualTo("ORDER_DETAIL");
        assertThat(vo.getActionLabel()).isEqualTo("查看订单");
        assertThat(vo.getActionUrl()).isEqualTo("/order/SO202607050001");
    }

    @Test
    void refundNotificationShouldPreferOrderRefundActionWhenOrderNoExists() {
        UserNotification notification = notification("REFUND", "订单 SO202607050001 的退款申请 RA202607050001 已提交");

        NotificationVO vo = NotificationVO.from(notification);

        assertThat(vo.getActionType()).isEqualTo("REFUND_DETAIL");
        assertThat(vo.getActionLabel()).isEqualTo("查看售后");
        assertThat(vo.getActionUrl()).isEqualTo("/orders/SO202607050001/refund");
    }

    @Test
    void couponNotificationShouldExposeCouponCenterAction() {
        UserNotification notification = notification("COUPON", "您有新的优惠券可领取");

        NotificationVO vo = NotificationVO.from(notification);

        assertThat(vo.getActionType()).isEqualTo("COUPON_CENTER");
        assertThat(vo.getActionLabel()).isEqualTo("查看优惠券");
        assertThat(vo.getActionUrl()).isEqualTo("/coupons");
    }

    @Test
    void systemNotificationWithoutBizNoShouldHaveNoAction() {
        UserNotification notification = notification("SYSTEM", "系统维护通知");

        NotificationVO vo = NotificationVO.from(notification);

        assertThat(vo.getActionType()).isNull();
        assertThat(vo.getActionLabel()).isNull();
        assertThat(vo.getActionUrl()).isNull();
    }

    private UserNotification notification(String category, String content) {
        UserNotification notification = new UserNotification();
        notification.setId(1L);
        notification.setTitle("通知标题");
        notification.setContent(content);
        notification.setCategory(category);
        notification.setReadStatus(0);
        return notification;
    }
}
