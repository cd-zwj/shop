package com.payment.service.delivery;

import com.payment.entity.OrderDeliveryRecord;
import com.payment.mapper.OrderDeliveryRecordMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

/** 对历史明文和旧 key 取货凭证执行在线轮换。 */
@Service
@RequiredArgsConstructor
public class PickupCodeRotationService {

    private final OrderDeliveryRecordMapper deliveryRecordMapper;
    private final PickupCodePayloadService payloadService;

    public int rotateAll(int batchSize) {
        if (batchSize < 1 || batchSize > 1000) {
            throw new IllegalArgumentException("取货码轮换批次必须在 1-1000 之间");
        }
        long cursor = 0L;
        int updatedCount = 0;
        while (true) {
            List<OrderDeliveryRecord> records =
                    deliveryRecordMapper.selectPickupCodeRotationBatch(cursor, batchSize);
            if (records.isEmpty()) {
                return updatedCount;
            }
            for (OrderDeliveryRecord record : records) {
                cursor = Math.max(cursor, record.getId());
                if (!payloadService.requiresRotation(record)) {
                    continue;
                }
                String oldPayload = record.getPayload();
                String encryptedPayload = payloadService.rotatePayload(record);
                int updated = deliveryRecordMapper.compareAndSetPickupCodePayload(
                        record.getId(), oldPayload, encryptedPayload);
                if (updated != 1) {
                    throw new IllegalStateException("取货码轮换遇到并发更新，请重新执行");
                }
                updatedCount++;
            }
        }
    }
}
