package com.payment.service.delivery;

import com.payment.enums.DeliveryStatusEnum;

/**
 * 单个订单项的交付结果。
 *
 * 同步交付（虚拟内容）直接返回 DELIVERED + payload；
 * 异步交付（卡密、订阅、第三方对接）返回 PENDING/DELIVERING，由后续步骤补 payload；
 * 失败返回 FAILED + failReason，由调用方决定是否重试。
 */
public record DeliveryResult(DeliveryStatusEnum status, String payload, String failReason) {

    public static DeliveryResult delivered(String payload) {
        return new DeliveryResult(DeliveryStatusEnum.DELIVERED, payload, null);
    }

    public static DeliveryResult pending(String payload) {
        return new DeliveryResult(DeliveryStatusEnum.PENDING, payload, null);
    }

    public static DeliveryResult failed(String reason) {
        return new DeliveryResult(DeliveryStatusEnum.FAILED, null, reason);
    }
}
