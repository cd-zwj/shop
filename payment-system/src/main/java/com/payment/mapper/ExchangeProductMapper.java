package com.payment.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.payment.entity.ExchangeProduct;
import org.apache.ibatis.annotations.Mapper;

/**
 * 积分兑换商品数据访问接口，提供积分兑换商品表（exchange_product）的 CRUD 操作。
 * 用于管理用户可使用积分兑换的虚拟/实物商品。
 */
@Mapper
public interface ExchangeProductMapper extends BaseMapper<ExchangeProduct> {
}
