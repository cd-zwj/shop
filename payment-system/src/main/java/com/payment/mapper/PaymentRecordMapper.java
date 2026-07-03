package com.payment.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.payment.entity.PaymentRecord;
import org.apache.ibatis.annotations.Mapper;

/**
 * 支付记录 Mapper
 * <p>对应表：payment_record，记录每笔支付渠道的交易明细（如微信/支付宝单次扣款记录）</p>
 *
 * @author payment-system
 */
@Mapper
public interface PaymentRecordMapper extends BaseMapper<PaymentRecord> {
}

