package com.payment.service.impl;

import com.payment.dto.PaymentCallbackDTO;
import com.payment.entity.PaymentCallbackFailureAudit;
import com.payment.enums.MessageProcessStatusEnum;
import com.payment.enums.PaymentCallbackFailureReasonEnum;
import com.payment.enums.PaymentChannelCodeEnum;
import com.payment.mapper.PaymentCallbackFailureAuditMapper;
import com.payment.service.PaymentCallbackAuditService;
import com.payment.util.PaymentCallbackPayloadUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PaymentCallbackAuditServiceImpl implements PaymentCallbackAuditService {

    private static final String UNRECOGNIZED_CHANNEL = "UNRECOGNIZED";
    private static final int MAX_BILL_NO_LENGTH = 64;
    private static final int MAX_PROVIDER_REQUEST_ID_LENGTH = 128;
    private static final int RETENTION_DAYS = 90;

    private final PaymentCallbackFailureAuditMapper auditMapper;

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordRejected(String channelCode,
                               PaymentCallbackDTO callbackDTO,
                               PaymentCallbackFailureReasonEnum failureReason) {
        PaymentCallbackDTO safeDto = callbackDTO == null ? new PaymentCallbackDTO() : callbackDTO;
        PaymentCallbackFailureReasonEnum safeReason = failureReason == null
                ? PaymentCallbackFailureReasonEnum.PAYLOAD_INVALID
                : failureReason;
        String rawBody = safeDto.getRawBody();

        PaymentCallbackFailureAudit audit = new PaymentCallbackFailureAudit();
        audit.setEventId("PFA" + UUID.randomUUID().toString().replace("-", ""));
        audit.setChannelCode(normalizeChannel(channelCode));
        audit.setFailureReason(safeReason.name());
        audit.setVerifyStatus(safeReason.isSignatureVerified()
                ? MessageProcessStatusEnum.SUCCESS.name()
                : MessageProcessStatusEnum.FAILED.name());
        audit.setCandidateBillNo(normalizeUntrustedIdentifier(safeDto.getBillNo(), MAX_BILL_NO_LENGTH));
        audit.setProviderRequestId(normalizeUntrustedIdentifier(
                safeDto.getCallbackRequestId(), MAX_PROVIDER_REQUEST_ID_LENGTH));
        audit.setPayloadSha256(PaymentCallbackPayloadUtil.sha256(rawBody));
        audit.setPayloadSize(PaymentCallbackPayloadUtil.byteSize(rawBody));
        LocalDateTime now = LocalDateTime.now().withNano(0);
        audit.setOccurrenceCount(1L);
        audit.setWindowStart(now.truncatedTo(ChronoUnit.MINUTES));
        audit.setCreateTime(now);
        audit.setLastTime(now);
        audit.setExpireTime(now.plusDays(RETENTION_DAYS));
        auditMapper.upsertWindow(audit);
    }

    private String normalizeChannel(String channelCode) {
        if (channelCode == null) {
            return UNRECOGNIZED_CHANNEL;
        }
        try {
            return PaymentChannelCodeEnum.valueOf(channelCode.trim().toUpperCase()).name();
        } catch (IllegalArgumentException e) {
            return UNRECOGNIZED_CHANNEL;
        }
    }

    private String normalizeUntrustedIdentifier(String value, int maxLength) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String normalized = value.trim();
        if (normalized.length() > maxLength || normalized.chars().anyMatch(Character::isISOControl)) {
            return null;
        }
        return normalized;
    }
}
