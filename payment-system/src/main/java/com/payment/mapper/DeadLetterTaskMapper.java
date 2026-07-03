package com.payment.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.payment.entity.DeadLetterTask;
import org.apache.ibatis.annotations.Mapper;

/**
 * 死信任务 Mapper 接口
 * <p>
 * 管理进入死信队列的消息任务记录。
 * 当消息多次重试仍失败后，会转入死信队列，由本 Mapper 负责持久化存储，
 * 供后续人工排查或补偿处理。
 * </p>
 *
 * @author payment-system
 */
@Mapper
public interface DeadLetterTaskMapper extends BaseMapper<DeadLetterTask> {
}
