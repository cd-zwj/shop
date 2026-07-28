package com.payment.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.payment.entity.MessageIdempotent;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

import java.time.LocalDateTime;

/**
 * 消息幂等记录 Mapper 接口
 * <p>
 * 用于消息消费的幂等性保障，防止重复消费导致的数据不一致问题。
 * 每条消息的唯一标识会在消费前查询，已消费的消息将被跳过。
 * </p>
 *
 * @author payment-system
 */
@Mapper
public interface MessageIdempotentMapper extends BaseMapper<MessageIdempotent> {

    @Update("""
            UPDATE message_idempotent
            SET status = 0,
                retry_count = retry_count + 1,
                error_message = #{claimMarker},
                message_body = #{messageBody},
                consumer_name = #{consumerName},
                updated_time = #{now}
            WHERE message_id = #{messageId}
              AND queue_name = #{queueName}
              AND status = 2
            """)
    int retryFailed(@Param("messageId") String messageId,
                    @Param("queueName") String queueName,
                    @Param("messageBody") String messageBody,
                    @Param("consumerName") String consumerName,
                    @Param("claimMarker") String claimMarker,
                    @Param("now") LocalDateTime now);

    @Update("""
            UPDATE message_idempotent
            SET retry_count = retry_count + 1,
                error_message = #{claimMarker},
                message_body = #{messageBody},
                consumer_name = #{consumerName},
                updated_time = #{now}
            WHERE message_id = #{messageId}
              AND queue_name = #{queueName}
              AND status = 0
              AND updated_time < #{staleBefore}
            """)
    int reclaimStale(@Param("messageId") String messageId,
                     @Param("queueName") String queueName,
                     @Param("messageBody") String messageBody,
                     @Param("consumerName") String consumerName,
                     @Param("claimMarker") String claimMarker,
                     @Param("now") LocalDateTime now,
                     @Param("staleBefore") LocalDateTime staleBefore);

    @Insert("""
            INSERT IGNORE INTO message_idempotent
                (message_id, queue_name, message_body, consumer_name, status, retry_count,
                 error_message, created_time, updated_time)
            VALUES
                (#{messageId}, #{queueName}, #{messageBody}, #{consumerName}, 0, 0,
                 #{claimMarker}, #{now}, #{now})
            """)
    int insertProcessing(@Param("messageId") String messageId,
                         @Param("queueName") String queueName,
                         @Param("messageBody") String messageBody,
                         @Param("consumerName") String consumerName,
                         @Param("claimMarker") String claimMarker,
                         @Param("now") LocalDateTime now);

    @Update("""
            UPDATE message_idempotent
            SET status = #{status},
                message_body = #{messageBody},
                consumer_name = #{consumerName},
                error_message = #{errorMessage},
                updated_time = #{now}
            WHERE message_id = #{messageId}
              AND queue_name = #{queueName}
              AND status = 0
              AND error_message = #{claimMarker}
            """)
    int finishProcessing(@Param("messageId") String messageId,
                         @Param("queueName") String queueName,
                         @Param("messageBody") String messageBody,
                         @Param("consumerName") String consumerName,
                         @Param("status") int status,
                         @Param("claimMarker") String claimMarker,
                         @Param("errorMessage") String errorMessage,
                         @Param("now") LocalDateTime now);
}
