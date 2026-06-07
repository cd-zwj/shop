package com.payment.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
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
            throw new RuntimeException("账号已被锁定，请" + LOCK_MINUTES + "分钟后重试");
        }
    }

    @Override
    public void recordFailure(String account, String ip) {
        LocalDateTime now = LocalDateTime.now();

        // 原子递增
        LambdaUpdateWrapper<LoginFailRecord> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(LoginFailRecord::getAccount, account)
                     .setSql("fail_count = fail_count + 1")
                     .set(LoginFailRecord::getLastFailTime, now)
                     .set(LoginFailRecord::getIp, ip);
        int affected = loginFailRecordMapper.update(null, updateWrapper);

        if (affected == 0) {
            // 首次失败，插入记录
            LoginFailRecord record = new LoginFailRecord();
            record.setAccount(account);
            record.setIp(ip);
            record.setFailCount(1);
            record.setLastFailTime(now);
            loginFailRecordMapper.insert(record);
        }

        // 检查是否达到锁定阈值
        LoginFailRecord current = findByAccount(account);
        if (current != null && current.getFailCount() >= MAX_FAIL_COUNT
                && current.getLockedUntil() == null) {
            LambdaUpdateWrapper<LoginFailRecord> lockWrapper = new LambdaUpdateWrapper<>();
            lockWrapper.eq(LoginFailRecord::getAccount, account)
                       .set(LoginFailRecord::getLockedUntil, now.plusMinutes(LOCK_MINUTES));
            loginFailRecordMapper.update(null, lockWrapper);
            log.warn("账号 {} 因连续{}次登录失败被锁定{}分钟", account, MAX_FAIL_COUNT, LOCK_MINUTES);
        }
    }

    @Override
    public void clearFailures(String account) {
        LambdaUpdateWrapper<LoginFailRecord> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(LoginFailRecord::getAccount, account);
        loginFailRecordMapper.delete(wrapper);
    }

    private LoginFailRecord findByAccount(String account) {
        LambdaQueryWrapper<LoginFailRecord> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(LoginFailRecord::getAccount, account).last("LIMIT 1");
        return loginFailRecordMapper.selectOne(wrapper);
    }
}
