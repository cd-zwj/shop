package com.payment.service.impl;

import com.payment.dto.PayResponseDTO;
import com.payment.dto.PaymentCallbackDTO;
import com.payment.entity.PaymentBill;
import com.payment.enums.PaymentChannelCodeEnum;
import com.payment.service.PaymentProvider;
import org.springframework.stereotype.Component;

/**
 * 外部支付渠道占位实现。
 *
 * 当前先返回一个可追踪的占位链接，后续接入真实第三方时保持接口不变。
 */
@Component
public class ExtPaymentProviderImpl implements PaymentProvider {

    @Override
    public String getChannelCode() {
        return PaymentChannelCodeEnum.EXT_PROVIDER.name();
    }

    @Override
    public PayResponseDTO createPayment(PaymentBill paymentBill) {
        PayResponseDTO responseDTO = new PayResponseDTO();
        responseDTO.setOrderNo(paymentBill.getBizNo());
        responseDTO.setPayType(paymentBill.getChannelCode());
        responseDTO.setAmount(paymentBill.getPayAmount());
        responseDTO.setPayUrl("https://placeholder-pay.local/pay?billNo=" + paymentBill.getBillNo());
        return responseDTO;
    }

    @Override
    public boolean verifyCallback(PaymentCallbackDTO callbackDTO) {
        return callbackDTO.getSuccess() != null && callbackDTO.getSuccess() && callbackDTO.getBillNo() != null;
    }
}
