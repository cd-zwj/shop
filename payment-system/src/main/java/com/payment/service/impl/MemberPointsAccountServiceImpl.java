package com.payment.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.payment.common.BusinessException;
import com.payment.entity.MemberPointsAccount;
import com.payment.entity.MemberPointsLog;
import com.payment.enums.PointsDeductStatusEnum;
import com.payment.mapper.MemberPointsAccountMapper;
import com.payment.mapper.MemberPointsLogMapper;
import com.payment.service.MemberPointsAccountService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * 会员积分账户服务实现类，用于实现会员积分账户相关业务逻辑。
 */
@Service
@RequiredArgsConstructor
public class MemberPointsAccountServiceImpl implements MemberPointsAccountService {

    private final MemberPointsAccountMapper accountMapper;
    private final MemberPointsLogMapper logMapper;

    /**
     * 获取账号。
     */
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

    /**
     * 查询流水。
     */
    @Override
    public Page<MemberPointsLog> listLogs(Long tenantId, Long platformUserId, Integer current, Integer size) {
        return logMapper.selectPage(new Page<>(current, size), new LambdaQueryWrapper<MemberPointsLog>()
                .eq(MemberPointsLog::getTenantId, tenantId)
                .eq(MemberPointsLog::getPlatformUserId, platformUserId)
                .orderByDesc(MemberPointsLog::getCreateTime));
    }

    /**
     * 处理grant积分。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void grantPoints(Long tenantId, Long platformUserId, Integer points, String bizType, String bizNo, String remark) {
        if (points == null || points <= 0) {
            return;
        }

        MemberPointsAccount account = getAccount(tenantId, platformUserId);
        Integer before = account.getPoints();
        Integer after = Math.addExact(before, points);

        account.setPoints(after);
        account.setTotalEarned(Math.addExact(account.getTotalEarned(), points));
        boolean updated = false;
        for (int retry = 0; retry < 3; retry++) {
            if (accountMapper.updateById(account) > 0) {
                updated = true;
                break;
            }
            // 乐观锁冲突，重读账户并重算
            account = accountMapper.selectById(account.getId());
            after = Math.addExact(account.getPoints(), points);
            account.setPoints(after);
            account.setTotalEarned(Math.addExact(account.getTotalEarned(), points));
        }
        if (!updated) {
            throw new BusinessException("积分发放失败，请重试");
        }

        MemberPointsLog log = new MemberPointsLog();
        log.setTenantId(tenantId);
        log.setPlatformUserId(platformUserId);
        log.setBizType(bizType);
        log.setBizNo(bizNo);
        log.setChangePoints(points);
        log.setPointsBefore(before);
        log.setPointsAfter(after);
        log.setStatus(PointsDeductStatusEnum.CONFIRMED.name());
        log.setRemark(remark);
        logMapper.insert(log);
    }

    /**
     * 预占积分。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public MemberPointsLog holdPoints(Long tenantId, Long platformUserId, Integer points, String bizType, String bizNo, String remark) {
        if (points == null || points <= 0) {
            throw new BusinessException("预占积分必须大于0");
        }

        MemberPointsAccount account = getAccount(tenantId, platformUserId);
        if (account.getPoints() < points) {
            throw new BusinessException("会员积分不足");
        }

        Integer before = account.getPoints();
        Integer after = Math.subtractExact(before, points);

        account.setPoints(after);
        account.setTotalUsed(Math.addExact(account.getTotalUsed(), points));
        boolean updated = false;
        for (int retry = 0; retry < 3; retry++) {
            if (accountMapper.updateById(account) > 0) {
                updated = true;
                break;
            }
            account = accountMapper.selectById(account.getId());
            if (account.getPoints() < points) {
                throw new BusinessException("会员积分不足");
            }
            after = Math.subtractExact(account.getPoints(), points);
            account.setPoints(after);
            account.setTotalUsed(Math.addExact(account.getTotalUsed(), points));
        }
        if (!updated) {
            throw new BusinessException("积分预占失败，请重试");
        }

        MemberPointsLog log = new MemberPointsLog();
        log.setTenantId(tenantId);
        log.setPlatformUserId(platformUserId);
        log.setBizType(bizType);
        log.setBizNo(bizNo);
        log.setChangePoints(-points);
        log.setPointsBefore(before);
        log.setPointsAfter(after);
        log.setStatus(PointsDeductStatusEnum.PRE_HOLD.name());
        log.setRemark(remark);
        logMapper.insert(log);
        return log;
    }

    /**
     * 确认积分预占。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void confirmPointsHold(Long tenantId, Long platformUserId, String bizType, String bizNo) {
        MemberPointsLog log = getPreHoldLog(tenantId, platformUserId, bizType, bizNo);
        if (log == null) {
            return;
        }
        log.setStatus(PointsDeductStatusEnum.CONFIRMED.name());
        log.setConfirmTime(LocalDateTime.now());
        logMapper.updateById(log);
    }

    /**
     * 释放积分预占。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void releasePointsHold(Long tenantId, Long platformUserId, String bizType, String bizNo, String releaseReason) {
        MemberPointsLog log = getPreHoldLog(tenantId, platformUserId, bizType, bizNo);
        if (log == null) {
            return;
        }

        MemberPointsAccount account = getAccount(tenantId, platformUserId);
        Integer holdPoints = Math.abs(log.getChangePoints());
        account.setPoints(Math.addExact(account.getPoints(), holdPoints));
        account.setTotalUsed(Math.max(0, account.getTotalUsed() - holdPoints));
        boolean updated = false;
        for (int retry = 0; retry < 3; retry++) {
            if (accountMapper.updateById(account) > 0) {
                updated = true;
                break;
            }
            account = accountMapper.selectById(account.getId());
            account.setPoints(Math.addExact(account.getPoints(), holdPoints));
            account.setTotalUsed(Math.max(0, account.getTotalUsed() - holdPoints));
        }
        if (!updated) {
            throw new BusinessException("积分释放失败，请重试");
        }

        log.setStatus(PointsDeductStatusEnum.RELEASED.name());
        log.setReleaseTime(LocalDateTime.now());
        log.setReleaseReason(releaseReason);
        logMapper.updateById(log);
    }

    private MemberPointsLog getPreHoldLog(Long tenantId, Long platformUserId, String bizType, String bizNo) {
        return logMapper.selectOne(new LambdaQueryWrapper<MemberPointsLog>()
                .eq(MemberPointsLog::getTenantId, tenantId)
                .eq(MemberPointsLog::getPlatformUserId, platformUserId)
                .eq(MemberPointsLog::getBizType, bizType)
                .eq(MemberPointsLog::getBizNo, bizNo)
                .eq(MemberPointsLog::getStatus, PointsDeductStatusEnum.PRE_HOLD.name()));
    }
}
