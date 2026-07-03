package com.payment.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.payment.common.BusinessException;
import com.payment.config.RabbitMQConfig;
import com.payment.entity.MemberPointsAccount;
import com.payment.entity.MemberPointsLog;
import com.payment.enums.PointsDeductStatusEnum;
import com.payment.mapper.MemberPointsAccountMapper;
import com.payment.mapper.MemberPointsLogMapper;
import com.payment.service.CompensationTaskFactory;
import com.payment.service.MemberPointsAccountService;
import com.payment.service.OutboxPublisher;
import com.payment.service.outbox.OutboxMessageCommand;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.ToIntFunction;

/**
 * 会员积分账户服务实现类。
 *
 * <p>负责会员积分账户的全生命周期管理，核心功能包括：</p>
 * <ul>
 *   <li><b>账户管理</b>：按租户+用户维度维护积分账户，支持自动创建（懒初始化）</li>
 *   <li><b>积分发放</b>：为用户增加积分，记录流水并支持过期时间设置</li>
 *   <li><b>积分预占/释放/确认</b>：下单时预占积分，支付成功后确认，取消时释放</li>
 *   <li><b>积分过期</b>：批量扫描到期积分并自动扣减，生成过期流水</li>
 *   <li><b>补偿机制</b>：乐观锁并发重试 + 失败时创建补偿任务，保障资金安全</li>
 *   <li><b>事件发布</b>：所有积分变动通过 Outbox 模式发布到消息队列</li>
 * </ul>
 */
@Service
@RequiredArgsConstructor
public class MemberPointsAccountServiceImpl implements MemberPointsAccountService {

    /** 积分过期业务类型标识 */
    private static final String POINTS_EXPIRE_BIZ_TYPE = "POINTS_EXPIRE";
    /** 积分发放补偿业务类型标识 */
    private static final String POINTS_GRANT_COMPENSATION_BIZ_TYPE = "POINTS_GRANT";
    /** 积分预占补偿业务类型标识 */
    private static final String POINTS_HOLD_COMPENSATION_BIZ_TYPE = "POINTS_HOLD";
    /** 积分释放补偿业务类型标识 */
    private static final String POINTS_RELEASE_COMPENSATION_BIZ_TYPE = "POINTS_RELEASE";

    private final MemberPointsAccountMapper accountMapper;
    private final MemberPointsLogMapper logMapper;
    private final CompensationTaskFactory compensationTaskFactory;
    private final OutboxPublisher outboxPublisher;

    /**
     * 获取指定租户下用户的积分账户，若不存在则自动创建。
     *
     * <p>采用懒初始化策略：首次查询时自动创建初始账户（积分为0、版本号为0、状态为启用）。</p>
     *
     * @param tenantId       租户ID
     * @param platformUserId 平台用户ID
     * @return 积分账户实体
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
     * 分页查询用户积分变动流水记录。
     *
     * @param tenantId       租户ID
     * @param platformUserId 平台用户ID
     * @param current        当前页码
     * @param size           每页条数
     * @return 分页后的积分流水列表，按创建时间倒序排列
     */
    @Override
    public Page<MemberPointsLog> listLogs(Long tenantId, Long platformUserId, Integer current, Integer size) {
        return logMapper.selectPage(new Page<>(current, size), new LambdaQueryWrapper<MemberPointsLog>()
                .eq(MemberPointsLog::getTenantId, tenantId)
                .eq(MemberPointsLog::getPlatformUserId, platformUserId)
                .orderByDesc(MemberPointsLog::getCreateTime));
    }

    /**
     * 发放积分（不指定过期时间）。
     *
     * <p>委托{@link #grantPoints(Long, Long, Integer, String, String, String, LocalDateTime)}执行，
     * 过期时间为null表示积分永不过期。</p>
     *
     * @param tenantId       租户ID
     * @param platformUserId 平台用户ID
     * @param points         发放积分数量，必须大于0
     * @param bizType        业务类型（如签到、活动奖励等）
     * @param bizNo          业务单号，用于幂等
     * @param remark         备注说明
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void grantPoints(Long tenantId, Long platformUserId, Integer points, String bizType, String bizNo, String remark) {
        grantPoints(tenantId, platformUserId, points, bizType, bizNo, remark, null);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void grantPoints(Long tenantId, Long platformUserId, Integer points, String bizType, String bizNo, String remark,
                            LocalDateTime expireTime) {
        if (points == null || points <= 0) {
            return;
        }

        MemberPointsAccount account = getAccount(tenantId, platformUserId);
        Integer before = account.getPoints();
        Integer after = updateAccountWithRetry(account, current -> Math.addExact(current.getPoints(), points),
                current -> current.setTotalEarned(Math.addExact(current.getTotalEarned(), points)),
                POINTS_GRANT_COMPENSATION_BIZ_TYPE,
                bizNo,
                "积分发放失败，请重试");

        MemberPointsLog log = new MemberPointsLog();
        log.setTenantId(tenantId);
        log.setPlatformUserId(platformUserId);
        log.setBizType(bizType);
        log.setBizNo(bizNo);
        log.setChangePoints(points);
        log.setPointsBefore(before);
        log.setPointsAfter(after);
        log.setStatus(PointsDeductStatusEnum.CONFIRMED.name());
        log.setExpireTime(expireTime);
        log.setRemark(remark);
        logMapper.insert(log);
        publishPointsEvent("POINTS_GRANTED", log);
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
        Integer after = updateAccountWithRetry(account, current -> {
                    if (current.getPoints() < points) {
                        throw new BusinessException("会员积分不足");
                    }
                    return Math.subtractExact(current.getPoints(), points);
                },
                current -> current.setTotalUsed(Math.addExact(current.getTotalUsed(), points)),
                POINTS_HOLD_COMPENSATION_BIZ_TYPE,
                bizNo,
                "积分预占失败，请重试");

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
        publishPointsEvent("POINTS_HOLD", log);
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
        publishPointsEvent("POINTS_HOLD_CONFIRMED", log);
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
        updateAccountWithRetry(account, current -> Math.addExact(current.getPoints(), holdPoints),
                current -> current.setTotalUsed(Math.max(0, current.getTotalUsed() - holdPoints)),
                POINTS_RELEASE_COMPENSATION_BIZ_TYPE,
                bizNo,
                "积分释放失败，请重试");

        log.setStatus(PointsDeductStatusEnum.RELEASED.name());
        log.setReleaseTime(LocalDateTime.now());
        log.setReleaseReason(releaseReason);
        logMapper.updateById(log);
        publishPointsEvent("POINTS_HOLD_RELEASED", log);
    }

    /**
     * 扫描并过期已到期积分，返回实际扣减积分数。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public int expirePoints(LocalDateTime expireBefore, int batchSize) {
        LocalDateTime cutoff = expireBefore == null ? LocalDateTime.now() : expireBefore;
        int limit = batchSize <= 0 ? 100 : batchSize;

        List<MemberPointsLog> expiredEarnLogs = logMapper.selectList(new LambdaQueryWrapper<MemberPointsLog>()
                .gt(MemberPointsLog::getChangePoints, 0)
                .eq(MemberPointsLog::getStatus, PointsDeductStatusEnum.CONFIRMED.name())
                .isNotNull(MemberPointsLog::getExpireTime)
                .le(MemberPointsLog::getExpireTime, cutoff)
                .orderByAsc(MemberPointsLog::getExpireTime)
                .last("LIMIT " + limit));

        int totalExpired = 0;
        for (MemberPointsLog sourceLog : expiredEarnLogs) {
            totalExpired += expireOneEarnLog(sourceLog, cutoff);
        }
        return totalExpired;
    }

    @Override
    public Integer getExpiringPoints(Long tenantId, Long platformUserId, LocalDateTime startTime, LocalDateTime endTime) {
        List<MemberPointsLog> expiringLogs = logMapper.selectList(new LambdaQueryWrapper<MemberPointsLog>()
                .eq(MemberPointsLog::getTenantId, tenantId)
                .eq(MemberPointsLog::getPlatformUserId, platformUserId)
                .gt(MemberPointsLog::getChangePoints, 0)
                .eq(MemberPointsLog::getStatus, PointsDeductStatusEnum.CONFIRMED.name())
                .isNotNull(MemberPointsLog::getExpireTime)
                .ge(startTime != null, MemberPointsLog::getExpireTime, startTime)
                .le(endTime != null, MemberPointsLog::getExpireTime, endTime));
        int expiring = expiringLogs.stream()
                .map(MemberPointsLog::getChangePoints)
                .filter(points -> points != null && points > 0)
                .mapToInt(Integer::intValue)
                .sum();
        int available = Math.max(0, getAccount(tenantId, platformUserId).getPoints());
        return Math.min(expiring, available);
    }

    private MemberPointsLog getPreHoldLog(Long tenantId, Long platformUserId, String bizType, String bizNo) {
        return logMapper.selectOne(new LambdaQueryWrapper<MemberPointsLog>()
                .eq(MemberPointsLog::getTenantId, tenantId)
                .eq(MemberPointsLog::getPlatformUserId, platformUserId)
                .eq(MemberPointsLog::getBizType, bizType)
                .eq(MemberPointsLog::getBizNo, bizNo)
                .eq(MemberPointsLog::getStatus, PointsDeductStatusEnum.PRE_HOLD.name()));
    }

    private int expireOneEarnLog(MemberPointsLog sourceLog, LocalDateTime expireTime) {
        MemberPointsLog update = new MemberPointsLog();
        update.setStatus(PointsDeductStatusEnum.EXPIRED.name());
        update.setReleaseTime(expireTime);
        update.setReleaseReason("积分到期自动过期");
        int marked = logMapper.update(update, new LambdaUpdateWrapper<MemberPointsLog>()
                .eq(MemberPointsLog::getId, sourceLog.getId())
                .eq(MemberPointsLog::getStatus, PointsDeductStatusEnum.CONFIRMED.name()));
        if (marked <= 0) {
            return 0;
        }

        PointsChange change = expireAccountWithRetry(
                getAccount(sourceLog.getTenantId(), sourceLog.getPlatformUserId()),
                sourceLog.getChangePoints(),
                POINTS_EXPIRE_BIZ_TYPE + "_" + sourceLog.getId());
        if (change.deducted <= 0) {
            return 0;
        }

        MemberPointsLog expireLog = new MemberPointsLog();
        expireLog.setTenantId(sourceLog.getTenantId());
        expireLog.setPlatformUserId(sourceLog.getPlatformUserId());
        expireLog.setBizType(POINTS_EXPIRE_BIZ_TYPE);
        expireLog.setBizNo(POINTS_EXPIRE_BIZ_TYPE + "_" + sourceLog.getId());
        expireLog.setChangePoints(-change.deducted);
        expireLog.setPointsBefore(change.before);
        expireLog.setPointsAfter(change.after);
        expireLog.setStatus(PointsDeductStatusEnum.CONFIRMED.name());
        expireLog.setRemark("积分到期自动过期");
        logMapper.insert(expireLog);
        publishPointsEvent("POINTS_EXPIRED", expireLog);
        return change.deducted;
    }

    private Integer updateAccountWithRetry(MemberPointsAccount account,
                                           ToIntFunction<MemberPointsAccount> nextPoints,
                                           Consumer<MemberPointsAccount> extraMutation,
                                           String compensationBizType,
                                           String compensationBizNo,
                                           String failureMessage) {
        MemberPointsAccount current = account;
        Integer after = null;
        for (int retry = 0; retry < 3; retry++) {
            after = nextPoints.applyAsInt(current);
            extraMutation.accept(current);
            current.setPoints(after);
            if (accountMapper.updateById(current) > 0) {
                return after;
            }
            current = accountMapper.selectById(current.getId());
        }
        compensationTaskFactory.createIfAbsent(compensationBizType, compensationBizNo, failureMessage);
        throw new BusinessException(failureMessage);
    }

    private PointsChange expireAccountWithRetry(MemberPointsAccount account, Integer sourcePoints, String compensationBizNo) {
        MemberPointsAccount current = account;
        for (int retry = 0; retry < 3; retry++) {
            int before = Math.max(0, current.getPoints());
            int deducted = Math.min(before, sourcePoints);
            if (deducted <= 0) {
                return new PointsChange(before, before, 0);
            }

            int after = Math.subtractExact(before, deducted);
            current.setPoints(after);
            current.setTotalUsed(Math.addExact(current.getTotalUsed(), deducted));
            if (accountMapper.updateById(current) > 0) {
                return new PointsChange(before, after, deducted);
            }
            current = accountMapper.selectById(current.getId());
        }
        String failureMessage = "积分过期扣减失败，请重试";
        compensationTaskFactory.createIfAbsent(POINTS_EXPIRE_BIZ_TYPE, compensationBizNo, failureMessage);
        throw new BusinessException(failureMessage);
    }

    private static class PointsChange {
        private final int before;
        private final int after;
        private final int deducted;

        private PointsChange(int before, int after, int deducted) {
            this.before = before;
            this.after = after;
            this.deducted = deducted;
        }
    }

    private void publishPointsEvent(String eventType, MemberPointsLog log) {
        outboxPublisher.publish(OutboxMessageCommand.builder()
                .messagePrefix("PTS")
                .bizType("POINTS_EVENT")
                .bizNo(log.getBizNo())
                .routingKey(RabbitMQConfig.POINTS_EVENT_QUEUE)
                .messageBody(Map.of(
                        "eventType", eventType,
                        "tenantId", log.getTenantId(),
                        "platformUserId", log.getPlatformUserId(),
                        "bizType", log.getBizType(),
                        "bizNo", log.getBizNo(),
                        "changePoints", log.getChangePoints(),
                        "pointsBefore", log.getPointsBefore(),
                        "pointsAfter", log.getPointsAfter(),
                        "status", log.getStatus(),
                        "logId", log.getId() == null ? "" : log.getId()))
                .build());
    }
}
