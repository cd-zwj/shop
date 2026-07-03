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

/**
 * 图形验证码服务实现类。
 * <p>
 * 基于 Hutool LineCaptcha 生成图形验证码，使用 Redis 存储验证码答案并设置 5 分钟过期。
 * 通过 Redis BloomFilter 实现验证码 Key 的快速存在性校验，防止无效 Key 查询穿透。
 * 支持基于客户端 IP 的频率限制（每分钟最多 10 次），防止验证码接口被恶意刷取。
 * </p>
 *
 * @see AuthCaptchaService
 */
@Service
@RequiredArgsConstructor
public class AuthCaptchaServiceImpl implements AuthCaptchaService {

    /** Redis 验证码存储 Key 前缀 */
    private static final String CAPTCHA_KEY_PREFIX = "auth:captcha:";

    /** IP 限流计数 Key 前缀 */
    private static final String CAPTCHA_RATE_LIMIT_PREFIX = "auth:captcha:rate:";

    /** BloomFilter Key，用于快速校验验证码 Key 是否存在 */
    private static final String CAPTCHA_BLOOM_FILTER_KEY = "auth:captcha:bloom";

    /** 验证码字符集，排除易混淆字符（0/O/1/I/L） */
    private static final String CAPTCHA_CHARSET = "23456789ABCDEFGHJKLMNPQRSTUVWXYZ";

    /** 验证码长度 */
    private static final int CAPTCHA_LENGTH = 4;

    /** 验证码在 Redis 中的过期时间 */
    private static final Duration CAPTCHA_TTL = Duration.ofMinutes(5);

    /** IP 限流滑动窗口时长（秒） */
    private static final long CAPTCHA_RATE_LIMIT_WINDOW_SECONDS = 60L;

    /** IP 限流窗口内最大请求数 */
    private static final long CAPTCHA_RATE_LIMIT_MAX_REQUESTS = 10L;

    /** BloomFilter 预期插入量 */
    private static final long CAPTCHA_BLOOM_EXPECTED_INSERTIONS = 200_000L;

    /** BloomFilter 误判概率 */
    private static final double CAPTCHA_BLOOM_FALSE_PROBABILITY = 0.01D;

    /** BloomFilter 过期时间（小时） */
    private static final long CAPTCHA_BLOOM_TTL_HOURS = 48L;

    private final RedisUtils redisUtils;

    /**
     * 生成图形验证码。
     * <p>
     * 先执行 IP 频率校验和 BloomFilter 初始化，然后生成线条干扰验证码，
     * 将验证码答案存入 Redis（5 分钟过期），并将 Key 加入 BloomFilter。
     *
     * @param clientIp 客户端 IP 地址，用于限流统计
     * @return 包含验证码唯一 Key 和 Base64 编码图片数据的 VO
     * @throws BusinessException 当 IP 请求频率超限时抛出
     */
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

    /**
     * 校验用户输入的验证码。
     * <p>
     * 校验流程：参数非空检查 → BloomFilter 快速过滤无效 Key → Redis 获取并删除验证码 →
     * 忽略大小写比对验证码答案。验证码使用后立即从 Redis 删除，防止重放。
     *
     * @param captchaKey  验证码唯一 Key（由 generateCaptcha 返回）
     * @param captchaCode 用户输入的验证码内容
     * @throws BusinessException 验证码缺失、不存在、已过期或答案错误时抛出
     */
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

    /**
     * 构建验证码在 Redis 中的完整 Key。
     *
     * @param captchaKey 验证码唯一标识
     * @return Redis Key，格式为 auth:captcha:{captchaKey}
     */
    private String buildRedisKey(String captchaKey) {
        return CAPTCHA_KEY_PREFIX + captchaKey;
    }

    /**
     * 执行基于客户端 IP 的频率限制。
     * <p>
     * 使用 Redis INCR 实现滑动窗口计数，当窗口内请求数超过阈值时拒绝服务。
     *
     * @param clientIp 客户端 IP 地址
     * @throws BusinessException 当请求频率超过限制时抛出
     */
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

    /** 初始化 BloomFilter，若已存在则续期 TTL。 */
    private void prepareBloomFilter() {
        redisUtils.bloomFilterTryInit(
                CAPTCHA_BLOOM_FILTER_KEY,
                CAPTCHA_BLOOM_EXPECTED_INSERTIONS,
                CAPTCHA_BLOOM_FALSE_PROBABILITY
        );
        redisUtils.bloomFilterExpire(CAPTCHA_BLOOM_FILTER_KEY, CAPTCHA_BLOOM_TTL_HOURS, TimeUnit.HOURS);
    }

    /**
     * 标准化客户端 IP：空白替换为 "unknown"，冒号和点替换为下划线以适配 Redis Key。
     *
     * @param clientIp 原始客户端 IP
     * @return 标准化后的 IP 字符串
     */
    private String normalizeClientIp(String clientIp) {
        String normalizedIp = StrUtil.blankToDefault(clientIp, "unknown").trim();
        return normalizedIp.replace(":", "_").replace(".", "_");
    }

    /** 将字符串去空格并转大写，用于验证码忽略大小写比对。 */
    private String normalize(String value) {
        return StrUtil.trimToEmpty(value).toUpperCase();
    }

    /** 将验证码图片编码为 data:image/png;base64 格式的 Data URL。 */
    private String toDataUrl(LineCaptcha captcha) {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        captcha.write(outputStream);
        String encoded = Base64.getEncoder().encodeToString(outputStream.toByteArray());
        return "data:image/png;base64," + encoded;
    }
}
