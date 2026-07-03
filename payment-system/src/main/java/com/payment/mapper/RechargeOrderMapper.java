package com.payment.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.payment.entity.RechargeOrder;
import org.apache.ibatis.annotations.Mapper;

/**
 * 充值订单 Mapper
 * <p>对应表：recharge_order，记录商户钱包充值订单信息</p>
 *
 * @author payment-system
 */
@Mapper
public interface RechargeOrderMapper extends BaseMapper<RechargeOrder> {
}
