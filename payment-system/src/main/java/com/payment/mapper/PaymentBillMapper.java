package com.payment.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.payment.entity.PaymentBill;
import org.apache.ibatis.annotations.Mapper;

/**
 * 支付账单 Mapper
 * <p>对应表：payment_bill，记录支付流水的分账账单信息</p>
 *
 * @author payment-system
 */
@Mapper
public interface PaymentBillMapper extends BaseMapper<PaymentBill> {
}
