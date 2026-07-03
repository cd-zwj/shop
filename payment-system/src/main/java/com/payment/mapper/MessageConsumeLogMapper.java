package com.payment.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.payment.entity.MessageConsumeLog;
import org.apache.ibatis.annotations.Mapper;

/**
 * 消息消费日志 Mapper 接口
 * <p>
 * 记录消息的消费过程，包括消费时间、消费状态、异常信息等，
 * 用于消息消费的可追溯性和问题排查。
 * </p>
 *
 * @author payment-system
 */
@Mapper
public interface MessageConsumeLogMapper extends BaseMapper<MessageConsumeLog> {
}
