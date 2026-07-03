package com.payment.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.payment.entity.MemberPointsAccount;
import com.payment.entity.MemberPointsLog;

import java.time.LocalDateTime;

/**
 * 会员积分账户服务接口。
 *
 * <p>提供会员积分账户的精细化管理能力，与 {@link PointsService} 不同，
 * 本服务支持积分的预占/确认/释放三阶段事务模型，以及积分过期机制。
 * 适用于需要精确控制积分生命周期的场景（如订单积分预占）。</p>
 */
public interface MemberPointsAccountService {

    /**
     * 查询会员积分账户信息。
     *
     * @param tenantId       租户ID
     * @param platformUserId 平台用户ID
     * @return 积分账户实体，包含可用积分、冻结积分等余额信息
     */
    MemberPointsAccount getAccount(Long tenantId, Long platformUserId);

    /**
     * 分页查询会员积分变更日志。
     *
     * @param tenantId       租户ID
     * @param platformUserId 平台用户ID
     * @param current        页码
     * @param size           每页条数
     * @return 积分日志分页数据
     */
    Page<MemberPointsLog> listLogs(Long tenantId, Long platformUserId, Integer current, Integer size);

    /**
     * 直接发放积分（永不过期）。
     *
     * @param tenantId       租户ID
     * @param platformUserId 平台用户ID
     * @param points         积分数量（正整数）
     * @param bizType        业务类型
     * @param bizNo          业务单号（幂等键）
     * @param remark         备注说明
     */
    void grantPoints(Long tenantId, Long platformUserId, Integer points, String bizType, String bizNo, String remark);

    /**
     * 发放积分（指定过期时间）。
     *
     * @param tenantId       租户ID
     * @param platformUserId 平台用户ID
     * @param points         积分数量（正整数）
     * @param bizType        业务类型
     * @param bizNo          业务单号（幂等键）
     * @param remark         备注说明
     * @param expireTime     积分过期时间，到期后自动失效
     */
    void grantPoints(Long tenantId, Long platformUserId, Integer points, String bizType, String bizNo, String remark,
                     LocalDateTime expireTime);

    /**
     * 预占积分（冻结积分，待后续确认或释放）。
     *
     * <p>订单创建时调用，将可用积分转为冻结状态，防止被其他订单使用。</p>
     *
     * @param tenantId       租户ID
     * @param platformUserId 平台用户ID
     * @param points         预占积分数量（正整数）
     * @param bizType        业务类型
     * @param bizNo          业务单号（幂等键）
     * @param remark         备注说明
     * @return 积分变更日志记录
     * @throws com.payment.common.exception.BusinessException 当可用积分不足时抛出
     */
    MemberPointsLog holdPoints(Long tenantId, Long platformUserId, Integer points, String bizType, String bizNo, String remark);

    /**
     * 确认预占积分（订单支付成功后调用，积分正式扣除）。
     *
     * @param tenantId       租户ID
     * @param platformUserId 平台用户ID
     * @param bizType        业务类型
     * @param bizNo          业务单号（匹配之前的预占记录）
     */
    void confirmPointsHold(Long tenantId, Long platformUserId, String bizType, String bizNo);

    /**
     * 释放预占积分（订单取消后调用，冻结积分恢复为可用）。
     *
     * @param tenantId       租户ID
     * @param platformUserId 平台用户ID
     * @param bizType        业务类型
     * @param bizNo          业务单号（匹配之前的预占记录）
     * @param releaseReason  释放原因
     */
    void releasePointsHold(Long tenantId, Long platformUserId, String bizType, String bizNo, String releaseReason);

    /**
     * 扫描并过期已到期的积分。
     *
     * <p>由定时任务调用，将超过过期时间的有效积分标记为过期。</p>
     *
     * @param expireBefore 过期截止时间，早于此时间的积分将被处理
     * @param batchSize    批处理大小（分批过期，避免长时间锁表）
     * @return 本次过期处理的记录数
     */
    int expirePoints(LocalDateTime expireBefore, int batchSize);

    /**
     * 查询指定时间范围内即将过期的积分数量。
     *
     * @param tenantId       租户ID
     * @param platformUserId 平台用户ID
     * @param startTime      起始时间
     * @param endTime        截止时间
     * @return 即将过期的积分数量
     */
    Integer getExpiringPoints(Long tenantId, Long platformUserId, LocalDateTime startTime, LocalDateTime endTime);
}
