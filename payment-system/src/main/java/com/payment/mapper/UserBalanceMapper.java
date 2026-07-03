package com.payment.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.payment.entity.UserBalance;
import org.apache.ibatis.annotations.Mapper;

/**
 * 用户余额数据访问接口，提供用户余额表（user_balance）的 CRUD 操作。
 * 管理用户的统一钱包账户余额，包括余额查询、充值、扣减等操作。
 */
@Mapper
public interface UserBalanceMapper extends BaseMapper<UserBalance> {
}
