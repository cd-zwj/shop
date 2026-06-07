package com.payment.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.payment.entity.LoginFailRecord;
import org.apache.ibatis.annotations.Mapper;

/**
 * 登录失败记录Mapper
 */
@Mapper
public interface LoginFailRecordMapper extends BaseMapper<LoginFailRecord> {
}
