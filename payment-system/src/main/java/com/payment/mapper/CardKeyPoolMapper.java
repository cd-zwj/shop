package com.payment.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.payment.entity.CardKeyPool;
import org.apache.ibatis.annotations.Mapper;

/**
 * 卡密池数据访问接口，提供卡密池表（card_key_pool）的 CRUD 操作。
 * 用于虚拟商品（如充值卡、兑换码）的卡密管理。
 */
@Mapper
public interface CardKeyPoolMapper extends BaseMapper<CardKeyPool> {
}
