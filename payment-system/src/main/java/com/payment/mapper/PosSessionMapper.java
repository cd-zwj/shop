package com.payment.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.payment.entity.PosSession;
import org.apache.ibatis.annotations.Mapper;

/**
 * POS会话Mapper
 */
@Mapper
public interface PosSessionMapper extends BaseMapper<PosSession> {
}
