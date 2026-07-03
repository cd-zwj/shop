package com.payment.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.payment.entity.PaymentCallbackRecord;
import org.apache.ibatis.annotations.Mapper;

/**
 * 支付回调记录 Mapper
 * <p>对应表：payment_callback_record，记录支付渠道（微信/支付宝）的异步回调通知</p>
 *
 * @author payment-system
 */
@Mapper
public interface PaymentCallbackRecordMapper extends BaseMapper<PaymentCallbackRecord> {
}
