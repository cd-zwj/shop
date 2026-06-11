package com.payment.service.sms;

/**
 * 短信发送器接口，用于抽象短信供应商。
 */
public interface SmsSender {

    /**
     * 发送短信验证码。
     *
     * @param phone 手机号
     * @param code  验证码
     */
    void send(String phone, String code);
}
