package com.payment.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.payment.entity.MessageIdempotent;
import org.apache.ibatis.annotations.Mapper;

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
}
