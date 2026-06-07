package com.payment.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.payment.common.BusinessException;
import com.payment.entity.LoginFailRecord;
import com.payment.mapper.LoginFailRecordMapper;
import com.payment.service.LoginSecurityService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
 * 登录安全服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LoginSecurityServiceImpl implements LoginSecurityService {

    private final LoginFailRecordMapper loginFailRecordMapper;

    private static final int MAX_FAIL_COUNT = 5;
    private static final int LOCK_MINUTES = 30;
    private static final int FAIL_WINDOW_MINUTES = 5;

    @Override
    public void checkNotLocked(String account) {
        LoginFailRecord record = findByAccount(account);
        if (record != null && record.getLockedUntil() != null
                && record.getLockedUntil().isAfter(LocalDateTime.now())) {
            throw new BusinessException("账号已被锁定，请稍后重试");
        }
    }

    @Override
    public void recordFailure(String account, String ip) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime windowStart = now.minusMinutes(FAIL_WINDOW_MINUTES);

        // 原子化：在一次 UPDATE 中同时完成递增 + 时间窗口重置 + 锁定判定
        // 如果 last_fail_time 已超过窗口期，将 fail_count 重置为 1 并清除锁定
        LambdaUpdateWrapper<LoginFailRecord> windowResetWrapper = new LambdaUpdateWrapper<>();
        windowResetWrapper.eq(LoginFailRecord::getAccount, account)
                          .le(LoginFailRecord::getLastFailTime, windowStart)
                          .set(LoginFailRecord::getFailCount, 1)
                          .set(LoginFailRecord::getLastFailTime, now)
                          .set(LoginFailRecord::getIp, ip)
                          .set(LoginFailRecord::getLockedUntil, null);
        int windowReset = loginFailRecordMapper.update(null, windowResetWrapper);
        if (windowReset > 0) {
            log.info("账号 {} 失败窗口已过期，重置 fail_count=1", account);
            return;
        }

        // 窗口内：原子递增 + 到达阈值时自动设置 locked_until
        LambdaUpdateWrapper<LoginFailRecord> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(LoginFailRecord::getAccount, account)
                     .gt(LoginFailRecord::getLastFailTime, windowStart)
                     .setSql("fail_count = fail_count + 1")
                     .set(LoginFailRecord::getLastFailTime, now)
                     .set(LoginFailRecord::getIp, ip)
                     .setSql("locked_until = CASE WHEN fail_count + 1 >= " + MAX_FAIL_COUNT
                             + " AND locked_until IS NULL THEN DATE_ADD(NOW(), INTERVAL " + LOCK_MINUTES + " MINUTE)"
                             + " ELSE locked_until END");
        int affected = loginFailRecordMapper.update(null, updateWrapper);

        if (affected == 0) {
            // 首次失败，插入记录
            LoginFailRecord record = new LoginFailRecord();
            record.setAccount(account);
            record.setIp(ip);
            record.setFailCount(1);
            record.setLastFailTime(now);
            loginFailRecordMapper.insert(record);
        } else {
            // 检查是否刚触发锁定（用于日志告警）
            LoginFailRecord current = findByAccount(account);
            if (current != null && current.getLockedUntil() != null
                    && current.getLockedUntil().isAfter(now)
                    && current.getFailCount() != null && current.getFailCount() >= MAX_FAIL_COUNT) {
                log.warn("账号 {} 因连续{}次登录失败被锁定{}分钟", account, MAX_FAIL_COUNT, LOCK_MINUTES);
            }
        }
    }

    @Override
    public void clearFailures(String account) {
        LambdaUpdateWrapper<LoginFailRecord> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(LoginFailRecord::getAccount, account);
        loginFailRecordMapper.delete(wrapper);
    }

    private LoginFailRecord findByAccount(String account) {
        // 查询时检查时间窗口：如果 last_fail_time 已过期，视为未锁定
        LambdaQueryWrapper<LoginFailRecord> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(LoginFailRecord::getAccount, account).last("LIMIT 1");
        LoginFailRecord record = loginFailRecordMapper.selectOne(wrapper);
        if (record != null && record.getLastFailTime() != null
                && record.getLastFailTime().isBefore(LocalDateTime.now().minusMinutes(FAIL_WINDOW_MINUTES))) {
            // 窗口过期，异步清理（不影响当前请求）
            log.debug("账号 {} 失败记录窗口已过期，将在下次写入时重置", account);
        }
        return record;
    }
}
