package com.payment.service.delivery;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

/** 仅在显式开关启用时执行历史取货码轮换。 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PickupCodeRotationRunner implements ApplicationRunner {

    private final PickupCodeRotationService rotationService;

    @Value("${app.pickup-code.crypto.rotate-on-startup:false}")
    private boolean enabled;

    @Value("${app.pickup-code.crypto.rotation-batch-size:200}")
    private int batchSize;

    @Override
    public void run(ApplicationArguments args) {
        if (!enabled) {
            return;
        }
        int updated = rotationService.rotateAll(batchSize);
        log.info("取货码加密轮换完成，更新记录数={}", updated);
    }
}
