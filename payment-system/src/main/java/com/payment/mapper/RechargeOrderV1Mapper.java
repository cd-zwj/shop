package com.payment.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.payment.entity.RechargeOrderV1;
import org.apache.ibatis.annotations.Mapper;

/**
 * 充值订单 V1 Mapper
 * <p>对应表：recharge_order_v1，V1 版本充值订单的兼容记录</p>
 *
 * @author payment-system
 */
@Mapper
public interface RechargeOrderV1Mapper extends BaseMapper<RechargeOrderV1> {
}
