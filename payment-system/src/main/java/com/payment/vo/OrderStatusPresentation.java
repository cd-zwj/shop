package com.payment.vo;

import com.payment.entity.SalesOrder;
import com.payment.entity.SalesOrderItem;

import java.util.List;

/**
 * 订单状态展示文案与可操作动作。
 */
public final class OrderStatusPresentation {

    private OrderStatusPresentation() {
    }

    public static StatusPresentation from(SalesOrder order) {
        return from(order, List.of(), null, null);
    }

    public static StatusPresentation from(SalesOrder order,
                                          List<SalesOrderItem> items,
                                          String paymentBillStatus,
                                          String paymentBillStatusRemark) {
        if (order == null) {
            return new StatusPresentation("加载中", "正在同步订单状态。", "请稍候，系统正在读取最新订单信息。", null, List.of("DETAIL"));
        }

        if ("FAILED".equals(order.getPayStatus()) || "FAILED".equals(paymentBillStatus)) {
            String reason = hasText(paymentBillStatusRemark) ? paymentBillStatusRemark : "支付渠道返回失败或本地支付单处理失败";
            return new StatusPresentation(
                    "支付失败",
                    "失败原因：" + reason,
                    "下一步：返回订单详情重新发起支付；如已扣款，请联系商户并保留支付单号。",
                    reason,
                    List.of("PAY", "CONTACT_MERCHANT", "REPURCHASE"));
        }

        if ("EXPIRED".equals(paymentBillStatus) || "CLOSED".equals(paymentBillStatus)) {
            boolean expired = "EXPIRED".equals(paymentBillStatus);
            String reason = hasText(paymentBillStatusRemark)
                    ? paymentBillStatusRemark
                    : (expired ? "支付单已超过可支付时间" : "支付单已关闭，原链接不可继续使用");
            return new StatusPresentation(
                    expired ? "支付已过期" : "支付已关闭",
                    (expired ? "过期说明：" : "关闭原因：") + reason,
                    "下一步：重新发起支付，系统会创建新的本地支付单；如状态有疑问可联系商户核对。",
                    reason,
                    List.of("PAY", "CONTACT_MERCHANT"));
        }

        if ("PAYING".equals(order.getPayStatus()) || "PAYING".equals(paymentBillStatus)) {
            return new StatusPresentation(
                    "支付中",
                    "支付单已创建并进入支付确认阶段，正在等待本地同步支付结果。",
                    "下一步：保持支付状态页打开或手动刷新；长时间无结果可返回订单重新发起支付。",
                    null,
                    List.of("PAY", "CONTACT_MERCHANT"));
        }

        if (isPendingPayment(order)) {
            return new StatusPresentation(
                    "待支付",
                    "订单已创建，请在支付关闭前完成付款；如支付页丢失，可继续支付。",
                    "下一步：继续支付或取消订单。",
                    null,
                    List.of("PAY", "CANCEL"));
        }

        if (isClosed(order)) {
            String label = "CANCELLED".equals(order.getOrderStatus()) ? "已取消" : "已关闭";
            return new StatusPresentation(
                    label,
                    "当前订单已结束，如仍需购买可重新加入购物车。",
                    "下一步：重新购买同款商品，或进入详情查看结束原因。",
                    null,
                    List.of("REPURCHASE", "DETAIL"));
        }

        if ("SUCCESS".equals(order.getPayStatus()) || "PAID".equals(order.getOrderStatus())) {
            return fromDelivery(items);
        }

        return new StatusPresentation(
                safe(order.getOrderStatus()) + " / " + safe(order.getPayStatus()),
                "该订单处于非常规状态，请进入详情确认后续处理方式。",
                "下一步：查看详情，或联系商户核对订单状态。",
                null,
                List.of("DETAIL", "CONTACT_MERCHANT"));
    }

    private static StatusPresentation fromDelivery(List<SalesOrderItem> items) {
        List<String> statuses = items == null ? List.of() : items.stream()
                .map(SalesOrderItem::getDeliveryStatus)
                .filter(OrderStatusPresentation::hasText)
                .toList();

        if (statuses.isEmpty()) {
            return new StatusPresentation(
                    "已支付",
                    "支付已完成，系统正在等待商家或交付任务接管订单。",
                    "下一步：等待商家发货、卡密交付或服务核销。",
                    null,
                    List.of("REFUND", "CONTACT_MERCHANT"));
        }

        if (statuses.stream().anyMatch(status -> "FAILED".equals(status) || "REVOKE_FAILED".equals(status))) {
            return new StatusPresentation(
                    "履约失败",
                    "订单已支付，但交付或撤销流程出现异常。",
                    "下一步：联系商户处理；商家可在待办中心查看异常订单或重试交付。",
                    "交付任务失败或资源撤销失败",
                    List.of("CONTACT_MERCHANT", "REFUND"));
        }

        if (statuses.stream().allMatch("CONFIRMED"::equals)) {
            return new StatusPresentation(
                    "已完成",
                    "商品或服务已确认完成，订单履约结束。",
                    "下一步：可继续查看已购内容、再次购买，或在售后期内申请售后。",
                    null,
                    List.of("REPURCHASE", "REFUND"));
        }

        if (statuses.stream().anyMatch(status -> "DELIVERED".equals(status) || "CONFIRMED".equals(status))) {
            return new StatusPresentation(
                    "已发货",
                    "商家已发货或虚拟内容已交付，可在订单或已购内容中查看。",
                    "下一步：确认收货、查看卡密/文件/核销码，或按需申请售后。",
                    null,
                    List.of("VIEW_DELIVERY", "REFUND"));
        }

        if (statuses.stream().anyMatch("DELIVERING"::equals)) {
            return new StatusPresentation(
                    "发货中",
                    "商家或系统正在处理发货、卡密发放或服务凭证生成。",
                    "预计节点：交付完成后会更新为已发货，并在已购内容中开放查看。",
                    null,
                    List.of("REFUND", "CONTACT_MERCHANT"));
        }

        return new StatusPresentation(
                "待发货",
                "支付已完成，订单正在等待商家发货或系统自动交付。",
                "下一步：等待商家处理；长时间无进展可联系商户。",
                null,
                List.of("REFUND", "CONTACT_MERCHANT"));
    }

    private static boolean isPendingPayment(SalesOrder order) {
        return !"CANCELLED".equals(order.getOrderStatus())
                && !"CLOSED".equals(order.getOrderStatus())
                && !"CLOSED".equals(order.getPayStatus())
                && ("WAIT_PAY".equals(order.getPayStatus())
                || "PAYING".equals(order.getPayStatus())
                || "CREATED".equals(order.getOrderStatus()));
    }

    private static boolean isClosed(SalesOrder order) {
        return "CANCELLED".equals(order.getOrderStatus())
                || "CLOSED".equals(order.getOrderStatus())
                || "CLOSED".equals(order.getPayStatus());
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private static String safe(String value) {
        return hasText(value) ? value : "--";
    }
}
