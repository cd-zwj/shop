package com.payment.service.impl;

import com.payment.service.AppOrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** 周期性关闭过期未支付订单，释放门店库存锁定。 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OrderExpiryScheduler {

    private final AppOrderService appOrderService;

    @Scheduled(fixedDelayString = "${payment.order.expire.fixed-delay-ms:30000}")
    public void expireUnpaidOrders() {
        int expired = appOrderService.expireUnpaidOrders();
        if (expired > 0) {
            log.info("Expired {} unpaid sales orders", expired);
        }
    }
}
