package com.payment.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.payment.entity.LoginFailRecord;
import org.apache.ibatis.annotations.Mapper;

/**
 * 登录失败记录数据访问接口，提供登录失败记录表（login_fail_record）的 CRUD 操作。
 * 记录登录失败次数和时间，用于实现账号锁定和防暴力破解策略。
 */
@Mapper
public interface LoginFailRecordMapper extends BaseMapper<LoginFailRecord> {
}
