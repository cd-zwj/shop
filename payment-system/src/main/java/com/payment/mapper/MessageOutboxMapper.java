package com.payment.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.payment.entity.MessageOutbox;
import org.apache.ibatis.annotations.Mapper;

/**
 * 发件箱消息 Mapper 接口
 * <p>
 * 用于 Outbox 模式下消息的持久化操作，
 * 确保业务操作与消息发送的一致性（最终一致性）。
 * </p>
 *
 * @author payment-system
 */
@Mapper
public interface MessageOutboxMapper extends BaseMapper<MessageOutbox> {
}
