package com.payment.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.payment.entity.MemberPointsAccount;
import com.payment.entity.MemberPointsLog;
import com.payment.mapper.MemberPointsAccountMapper;
import com.payment.mapper.MemberPointsLogMapper;
import com.payment.service.MemberPointsAccountService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 商户会员积分服务。
 */
@Service
@RequiredArgsConstructor
public class MemberPointsAccountServiceImpl implements MemberPointsAccountService {

    private final MemberPointsAccountMapper accountMapper;
    private final MemberPointsLogMapper logMapper;

    @Override
    public MemberPointsAccount getAccount(Long tenantId, Long platformUserId) {
        MemberPointsAccount account = accountMapper.selectOne(new LambdaQueryWrapper<MemberPointsAccount>()
                .eq(MemberPointsAccount::getTenantId, tenantId)
                .eq(MemberPointsAccount::getPlatformUserId, platformUserId));
        if (account != null) {
            return account;
        }

        MemberPointsAccount newAccount = new MemberPointsAccount();
        newAccount.setTenantId(tenantId);
        newAccount.setPlatformUserId(platformUserId);
        newAccount.setPoints(0);
        newAccount.setTotalEarned(0);
        newAccount.setTotalUsed(0);
        newAccount.setVersion(0);
        newAccount.setStatus(1);
        accountMapper.insert(newAccount);
        return newAccount;
    }

    @Override
    public Page<MemberPointsLog> listLogs(Long tenantId, Long platformUserId, Integer current, Integer size) {
        return logMapper.selectPage(new Page<>(current, size), new LambdaQueryWrapper<MemberPointsLog>()
                .eq(MemberPointsLog::getTenantId, tenantId)
                .eq(MemberPointsLog::getPlatformUserId, platformUserId)
                .orderByDesc(MemberPointsLog::getCreateTime));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void grantPoints(Long tenantId, Long platformUserId, Integer points, String bizType, String bizNo, String remark) {
        if (points == null || points <= 0) {
            return;
        }

        MemberPointsAccount account = getAccount(tenantId, platformUserId);
        Integer before = account.getPoints();
        Integer after = before + points;

        account.setPoints(after);
        account.setTotalEarned(account.getTotalEarned() + points);
        account.setVersion(account.getVersion() + 1);
        accountMapper.updateById(account);

        MemberPointsLog log = new MemberPointsLog();
        log.setTenantId(tenantId);
        log.setPlatformUserId(platformUserId);
        log.setBizType(bizType);
        log.setBizNo(bizNo);
        log.setChangePoints(points);
        log.setPointsBefore(before);
        log.setPointsAfter(after);
        log.setRemark(remark);
        logMapper.insert(log);
    }
}
