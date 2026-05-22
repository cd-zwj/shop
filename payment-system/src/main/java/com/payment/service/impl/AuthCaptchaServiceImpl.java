package com.payment.service.impl;

import cn.hutool.captcha.CaptchaUtil;
import cn.hutool.captcha.LineCaptcha;
import cn.hutool.captcha.generator.RandomGenerator;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import com.payment.common.BusinessException;
import com.payment.dto.LoginCaptchaVO;
import com.payment.service.AuthCaptchaService;
import com.payment.util.RedisUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.time.Duration;
import java.util.Base64;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class AuthCaptchaServiceImpl implements AuthCaptchaService {

    private static final String CAPTCHA_KEY_PREFIX = "auth:captcha:";
    private static final String CAPTCHA_RATE_LIMIT_PREFIX = "auth:captcha:rate:";
    private static final String CAPTCHA_BLOOM_FILTER_KEY = "auth:captcha:bloom";
    private static final String CAPTCHA_CHARSET = "23456789ABCDEFGHJKLMNPQRSTUVWXYZ";
    private static final int CAPTCHA_LENGTH = 4;
    private static final Duration CAPTCHA_TTL = Duration.ofMinutes(5);
    private static final long CAPTCHA_RATE_LIMIT_WINDOW_SECONDS = 60L;
    private static final long CAPTCHA_RATE_LIMIT_MAX_REQUESTS = 10L;
    private static final long CAPTCHA_BLOOM_EXPECTED_INSERTIONS = 200_000L;
    private static final double CAPTCHA_BLOOM_FALSE_PROBABILITY = 0.01D;
    private static final long CAPTCHA_BLOOM_TTL_HOURS = 48L;

    private final RedisUtils redisUtils;

    @Override
    public LoginCaptchaVO generateCaptcha(String clientIp) {
        enforceIpRateLimit(clientIp);
        prepareBloomFilter();

        LineCaptcha captcha = CaptchaUtil.createLineCaptcha(160, 48, CAPTCHA_LENGTH, 40);
        captcha.setGenerator(new RandomGenerator(CAPTCHA_CHARSET, CAPTCHA_LENGTH));
        captcha.createCode();

        String captchaKey = IdUtil.fastSimpleUUID();
        redisUtils.set(buildRedisKey(captchaKey), normalize(captcha.getCode()), CAPTCHA_TTL);
        redisUtils.bloomFilterAdd(CAPTCHA_BLOOM_FILTER_KEY, captchaKey);

        return new LoginCaptchaVO(captchaKey, toDataUrl(captcha));
    }

    @Override
    public void validateCaptcha(String captchaKey, String captchaCode) {
        if (StrUtil.isBlank(captchaKey) || StrUtil.isBlank(captchaCode)) {
            throw new BusinessException("请输入图形验证码");
        }
        if (!redisUtils.bloomFilterContains(CAPTCHA_BLOOM_FILTER_KEY, captchaKey)) {
            throw new BusinessException("验证码不存在或已失效");
        }

        String redisKey = buildRedisKey(captchaKey);
        String expectedCode = redisUtils.get(redisKey);
        redisUtils.delete(redisKey);

        if (StrUtil.isBlank(expectedCode)) {
            throw new BusinessException("验证码已过期，请重新获取");
        }
        if (!StrUtil.equalsIgnoreCase(expectedCode, normalize(captchaCode))) {
            throw new BusinessException("验证码错误，请重新获取");
        }
    }

    private String buildRedisKey(String captchaKey) {
        return CAPTCHA_KEY_PREFIX + captchaKey;
    }

    private void enforceIpRateLimit(String clientIp) {
        String normalizedIp = normalizeClientIp(clientIp);
        long requestCount = redisUtils.incrementAndGet(
                CAPTCHA_RATE_LIMIT_PREFIX + normalizedIp,
                CAPTCHA_RATE_LIMIT_WINDOW_SECONDS,
                TimeUnit.SECONDS
        );
        if (requestCount > CAPTCHA_RATE_LIMIT_MAX_REQUESTS) {
            throw new BusinessException("获取验证码过于频繁，请1分钟后再试");
        }
    }

    private void prepareBloomFilter() {
        redisUtils.bloomFilterTryInit(
                CAPTCHA_BLOOM_FILTER_KEY,
                CAPTCHA_BLOOM_EXPECTED_INSERTIONS,
                CAPTCHA_BLOOM_FALSE_PROBABILITY
        );
        redisUtils.bloomFilterExpire(CAPTCHA_BLOOM_FILTER_KEY, CAPTCHA_BLOOM_TTL_HOURS, TimeUnit.HOURS);
    }

    private String normalizeClientIp(String clientIp) {
        String normalizedIp = StrUtil.blankToDefault(clientIp, "unknown").trim();
        return normalizedIp.replace(":", "_").replace(".", "_");
    }

    private String normalize(String value) {
        return StrUtil.trimToEmpty(value).toUpperCase();
    }

    private String toDataUrl(LineCaptcha captcha) {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        captcha.write(outputStream);
        String encoded = Base64.getEncoder().encodeToString(outputStream.toByteArray());
        return "data:image/png;base64," + encoded;
    }
}
