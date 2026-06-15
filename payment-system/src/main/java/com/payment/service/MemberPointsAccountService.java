package com.payment.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.payment.entity.MemberPointsAccount;
import com.payment.entity.MemberPointsLog;

import java.time.LocalDateTime;

public interface MemberPointsAccountService {
    MemberPointsAccount getAccount(Long tenantId, Long platformUserId);

    Page<MemberPointsLog> listLogs(Long tenantId, Long platformUserId, Integer current, Integer size);

    void grantPoints(Long tenantId, Long platformUserId, Integer points, String bizType, String bizNo, String remark);

    void grantPoints(Long tenantId, Long platformUserId, Integer points, String bizType, String bizNo, String remark,
                     LocalDateTime expireTime);

    MemberPointsLog holdPoints(Long tenantId, Long platformUserId, Integer points, String bizType, String bizNo, String remark);

    void confirmPointsHold(Long tenantId, Long platformUserId, String bizType, String bizNo);

    void releasePointsHold(Long tenantId, Long platformUserId, String bizType, String bizNo, String releaseReason);

    int expirePoints(LocalDateTime expireBefore, int batchSize);

    Integer getExpiringPoints(Long tenantId, Long platformUserId, LocalDateTime startTime, LocalDateTime endTime);
}
