package com.payment.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.payment.entity.RetryTask;
import org.apache.ibatis.annotations.Mapper;

/**
 * 重试任务 Mapper 接口
 * <p>
 * 管理消息重试任务的持久化操作。
 * 当消息首次消费失败后，系统会创建重试任务记录，
 * 按照指数退避策略进行多次重试，直至成功或转入死信队列。
 * </p>
 *
 * @author payment-system
 */
@Mapper
public interface RetryTaskMapper extends BaseMapper<RetryTask> {
}
