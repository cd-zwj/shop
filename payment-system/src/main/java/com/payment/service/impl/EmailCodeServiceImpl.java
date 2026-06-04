package com.payment.service.impl;

import cn.hutool.core.util.RandomUtil;
import com.payment.common.BusinessException;
import com.payment.config.EmailAuthProperties;
import com.payment.enums.EmailCodeSceneEnum;
import com.payment.service.EmailCodeService;
import com.payment.util.RedisUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.mail.MailProperties;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

/**
 * 邮箱验证码服务实现类，用于实现邮箱验证码相关业务逻辑。
 */
@Service
@RequiredArgsConstructor
public class EmailCodeServiceImpl implements EmailCodeService {

    private static final String EMAIL_CODE_PREFIX = "auth:email:code:";
    private static final String EMAIL_COOLDOWN_PREFIX = "auth:email:cooldown:";
    private static final String EMAIL_DAILY_PREFIX = "auth:email:daily:";

    private final JavaMailSender javaMailSender;
    private final RedisUtils redisUtils;
    private final EmailAuthProperties emailAuthProperties;
    private final MailProperties mailProperties;

    /**
     * 发送编码。
     */
    @Override
    public void sendCode(String email, EmailCodeSceneEnum scene) {
        ensureEnabled();
        String normalizedEmail = normalizeEmail(email);
        String cooldownKey = buildCooldownKey(scene, normalizedEmail);
        if (redisUtils.exists(cooldownKey)) {
            throw new BusinessException("验证码发送过于频繁，请稍后再试");
        }

        long dailyCount = redisUtils.incrementAndGet(buildDailyKey(scene, normalizedEmail), 1, TimeUnit.DAYS);
        if (dailyCount > emailAuthProperties.getMaxDailySendCount()) {
            throw new BusinessException("今日验证码发送次数已达上限，请明天再试");
        }

        String code = RandomUtil.randomNumbers(6);
        redisUtils.set(buildCodeKey(scene, normalizedEmail), code, Duration.ofMinutes(emailAuthProperties.getCodeTtlMinutes()));
        redisUtils.set(cooldownKey, "1", Duration.ofSeconds(emailAuthProperties.getSendCooldownSeconds()));

        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(normalizedEmail);
        message.setFrom(resolveFromAddress());
        message.setSubject(buildSubject(scene));
        message.setText(buildContent(scene, code));
        javaMailSender.send(message);
    }

    /**
     * 校验编码。
     */
    @Override
    public void validateCode(String email, String code, EmailCodeSceneEnum scene, boolean consume) {
        String normalizedEmail = normalizeEmail(email);
        String redisKey = buildCodeKey(scene, normalizedEmail);
        String expectedCode = redisUtils.get(redisKey);
        if (!StringUtils.hasText(expectedCode)) {
            throw new BusinessException("邮箱验证码已过期，请重新获取");
        }
        if (!expectedCode.equalsIgnoreCase(code == null ? null : code.trim())) {
            throw new BusinessException("邮箱验证码错误");
        }
        if (consume) {
            redisUtils.delete(redisKey);
        }
    }

    /**
     * 处理ensureEnabled。
     */
    private void ensureEnabled() {
        if (!emailAuthProperties.isEnabled()) {
            throw new BusinessException("邮箱能力未开启，请联系管理员配置");
        }
        if (!StringUtils.hasText(resolveFromAddress()) || !StringUtils.hasText(mailProperties.getPassword())) {
            throw new BusinessException("邮箱发送配置不完整，请联系管理员配置");
        }
    }

    /**
     * 解析FromAddres。
     */
    private String resolveFromAddress() {
        if (StringUtils.hasText(emailAuthProperties.getFrom())) {
            return emailAuthProperties.getFrom().trim();
        }
        if (StringUtils.hasText(mailProperties.getUsername())) {
            return mailProperties.getUsername().trim();
        }
        return null;
    }

    /**
     * 构建Subject。
     */
    private String buildSubject(EmailCodeSceneEnum scene) {
        return emailAuthProperties.getSubjectPrefix() + scene.getSubject();
    }

    /**
     * 构建Content。
     */
    private String buildContent(EmailCodeSceneEnum scene, String code) {
        return "您好，您的" + scene.getActionText() + "验证码为：" + code
                + "，" + emailAuthProperties.getCodeTtlMinutes()
                + "分钟内有效。如非本人操作，请忽略此邮件。";
    }

    /**
     * 构建编码Key。
     */
    private String buildCodeKey(EmailCodeSceneEnum scene, String email) {
        return EMAIL_CODE_PREFIX + scene.getCode() + ":" + email;
    }

    /**
     * 构建CooldownKey。
     */
    private String buildCooldownKey(EmailCodeSceneEnum scene, String email) {
        return EMAIL_COOLDOWN_PREFIX + scene.getCode() + ":" + email;
    }

    /**
     * 构建DailyKey。
     */
    private String buildDailyKey(EmailCodeSceneEnum scene, String email) {
        return EMAIL_DAILY_PREFIX + scene.getCode() + ":" + email;
    }

    /**
     * 规范化邮箱。
     */
    private String normalizeEmail(String email) {
        return email == null ? null : email.trim().toLowerCase();
    }
}
