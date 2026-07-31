package com.payment.service.delivery;

import com.payment.entity.OrderDeliveryRecord;
import com.payment.mapper.OrderDeliveryRecordMapper;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

class PickupCodeRotationServiceTest {

    @Test
    void rotateAllShouldReplaceOnlyLegacyOrOldKeyPayloads() {
        OrderDeliveryRecordMapper mapper = mock(OrderDeliveryRecordMapper.class);
        PickupCodePayloadService payloadService = mock(PickupCodePayloadService.class);
        PickupCodeRotationService service = new PickupCodeRotationService(mapper, payloadService);
        OrderDeliveryRecord legacy = record(1L, "legacy");
        OrderDeliveryRecord current = record(2L, "current");
        when(mapper.selectPickupCodeRotationBatch(0L, 100)).thenReturn(List.of(legacy, current));
        when(mapper.selectPickupCodeRotationBatch(2L, 100)).thenReturn(List.of());
        when(payloadService.requiresRotation(legacy)).thenReturn(true);
        when(payloadService.requiresRotation(current)).thenReturn(false);
        when(payloadService.rotatePayload(legacy)).thenReturn("encrypted");
        when(mapper.compareAndSetPickupCodePayload(1L, "legacy", "encrypted")).thenReturn(1);

        int updated = service.rotateAll(100);

        assertThat(updated).isEqualTo(1);
        verify(payloadService).rotatePayload(legacy);
        verify(mapper).compareAndSetPickupCodePayload(1L, "legacy", "encrypted");
        verify(mapper).selectPickupCodeRotationBatch(0L, 100);
        verify(mapper).selectPickupCodeRotationBatch(2L, 100);
        verifyNoMoreInteractions(mapper);
    }

    @Test
    void rotateAllShouldFailWhenCompareAndSetDetectsConcurrentChange() {
        OrderDeliveryRecordMapper mapper = mock(OrderDeliveryRecordMapper.class);
        PickupCodePayloadService payloadService = mock(PickupCodePayloadService.class);
        PickupCodeRotationService service = new PickupCodeRotationService(mapper, payloadService);
        OrderDeliveryRecord legacy = record(1L, "legacy");
        when(mapper.selectPickupCodeRotationBatch(0L, 100)).thenReturn(List.of(legacy));
        when(payloadService.requiresRotation(legacy)).thenReturn(true);
        when(payloadService.rotatePayload(legacy)).thenReturn("encrypted");
        when(mapper.compareAndSetPickupCodePayload(1L, "legacy", "encrypted")).thenReturn(0);

        assertThatThrownBy(() -> service.rotateAll(100))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("取货码轮换遇到并发更新，请重新执行");
    }

    private OrderDeliveryRecord record(Long id, String payload) {
        OrderDeliveryRecord record = new OrderDeliveryRecord();
        record.setId(id);
        record.setPayload(payload);
        return record;
    }
}
