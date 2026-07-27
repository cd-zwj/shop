package com.payment.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.payment.entity.PaymentCallbackFailureAudit;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface PaymentCallbackFailureAuditMapper extends BaseMapper<PaymentCallbackFailureAudit> {

    @Insert("""
            INSERT INTO payment_callback_failure_audit (
                event_id, channel_code, failure_reason, verify_status,
                candidate_bill_no, provider_request_id, payload_sha256, payload_size,
                occurrence_count, window_start, create_time, last_time, expire_time
            ) VALUES (
                #{eventId}, #{channelCode}, #{failureReason}, #{verifyStatus},
                #{candidateBillNo}, #{providerRequestId}, #{payloadSha256}, #{payloadSize},
                #{occurrenceCount}, #{windowStart}, #{createTime}, #{lastTime}, #{expireTime}
            )
            ON DUPLICATE KEY UPDATE
                occurrence_count = occurrence_count + 1,
                last_time = VALUES(last_time),
                expire_time = VALUES(expire_time)
            """)
    int upsertWindow(PaymentCallbackFailureAudit audit);

    @Delete("DELETE FROM payment_callback_failure_audit "
            + "WHERE expire_time < CURRENT_TIMESTAMP LIMIT #{limit}")
    int deleteExpiredBatch(@Param("limit") int limit);
}
