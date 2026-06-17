package com.payment.service.impl;

import cn.hutool.core.util.RandomUtil;
import com.payment.common.BusinessException;
import com.payment.config.RabbitMQConfig;
import com.payment.config.SmsAuthProperties;
import com.payment.service.OutboxPublisher;
import com.payment.service.SmsCodeService;
import com.payment.service.outbox.OutboxMessageCommand;
import com.payment.util.RedisUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * 短信验证码服务实现类，用于实现短信验证码相关业务逻辑。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SmsCodeServiceImpl implements SmsCodeService {

    private static final String SMS_CODE_PREFIX = "auth:sms:code:login:";
    private static final String SMS_COOLDOWN_PREFIX = "auth:sms:cooldown:login:";
    private static final String SMS_DAILY_PREFIX = "auth:sms:daily:login:";

    private final RedisUtils redisUtils;
    private final SmsAuthProperties smsAuthProperties;
    private final OutboxPublisher outboxPublisher;

    /**
     * 发送登录编码。
     */
    @Override
    public void sendLoginCode(String phone) {
        ensureEnabled();
        String normalizedPhone = normalizePhone(phone);
        if (!StringUtils.hasText(normalizedPhone)) {
            throw new BusinessException("手机号不能为空");
        }

        String cooldownKey = SMS_COOLDOWN_PREFIX + normalizedPhone;
        if (redisUtils.exists(cooldownKey)) {
            throw new BusinessException("验证码发送过于频繁，请稍后再试");
        }

        long dailyCount = redisUtils.incrementAndGet(SMS_DAILY_PREFIX + normalizedPhone, 1, TimeUnit.DAYS);
        if (dailyCount > smsAuthProperties.getMaxDailySendCount()) {
            throw new BusinessException("今日验证码发送次数已达上限，请明天再试");
        }

        String code = RandomUtil.randomNumbers(6);
        redisUtils.set(SMS_CODE_PREFIX + normalizedPhone, code, Duration.ofMinutes(smsAuthProperties.getCodeTtlMinutes()));
        redisUtils.set(cooldownKey, "1", Duration.ofSeconds(smsAuthProperties.getSendCooldownSeconds()));

        publishSmsSendOutbox(normalizedPhone, code);
    }

    @Override
    public void retryLoginCode(String phone) {
        ensureEnabled();
        String normalizedPhone = normalizePhone(phone);
        if (!StringUtils.hasText(normalizedPhone)) {
            throw new BusinessException("手机号不能为空");
        }

        String code = redisUtils.get(SMS_CODE_PREFIX + normalizedPhone);
        if (!StringUtils.hasText(code)) {
            throw new BusinessException("短信验证码已过期，请重新获取");
        }

        publishSmsSendOutbox(normalizedPhone, code);
    }

    /**
     * 校验登录编码。
     */
    @Override
    public void validateLoginCode(String phone, String code, boolean consume) {
        String normalizedPhone = normalizePhone(phone);
        String redisKey = SMS_CODE_PREFIX + normalizedPhone;
        String expectedCode = redisUtils.get(redisKey);
        if (!StringUtils.hasText(expectedCode)) {
            throw new BusinessException("短信验证码已过期，请重新获取");
        }
        if (!expectedCode.equals(code == null ? null : code.trim())) {
            throw new BusinessException("短信验证码错误");
        }
        if (consume) {
            redisUtils.delete(redisKey);
        }
    }

    /**
     * 处理ensureEnabled。
     */
    private void ensureEnabled() {
        if (!smsAuthProperties.isEnabled()) {
            throw new BusinessException("短信能力未开启，请联系管理员配置");
        }
    }

    /**
     * 规范化手机号。
     */
    private String normalizePhone(String phone) {
        return phone == null ? null : phone.trim();
    }

    private void publishSmsSendOutbox(String phone, String code) {
        outboxPublisher.publish(OutboxMessageCommand.builder()
                .messagePrefix("SMS")
                .bizType("SMS_SEND")
                .bizNo("SMS_LOGIN_" + phone + "_" + code)
                .routingKey(RabbitMQConfig.SMS_SEND_QUEUE)
                .messageBody(Map.of(
                        "scene", "LOGIN_CODE",
                        "phone", phone,
                        "code", code))
                .build());
    }
}
