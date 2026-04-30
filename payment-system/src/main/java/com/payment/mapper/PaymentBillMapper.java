package com.payment.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.payment.entity.PaymentBill;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface PaymentBillMapper extends BaseMapper<PaymentBill> {
}
