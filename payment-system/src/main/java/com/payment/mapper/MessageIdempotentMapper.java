package com.payment.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.payment.entity.MessageIdempotent;
import org.apache.ibatis.annotations.Mapper;

/**
 * 消息幂等性记录Mapper
 */
@Mapper
public interface MessageIdempotentMapper extends BaseMapper<MessageIdempotent> {
}
