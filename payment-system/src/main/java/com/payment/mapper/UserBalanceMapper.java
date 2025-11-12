package com.payment.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.payment.entity.UserBalance;
import org.apache.ibatis.annotations.Mapper;

/**
 * 用户余额Mapper
 */
@Mapper
public interface UserBalanceMapper extends BaseMapper<UserBalance> {
}
