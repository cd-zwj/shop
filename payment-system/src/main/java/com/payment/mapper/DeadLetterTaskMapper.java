package com.payment.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.payment.entity.DeadLetterTask;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface DeadLetterTaskMapper extends BaseMapper<DeadLetterTask> {
}
