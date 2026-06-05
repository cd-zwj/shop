package com.payment.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.payment.entity.MemberPointsAccount;
import com.payment.entity.MemberPointsLog;

public interface MemberPointsAccountService {
    MemberPointsAccount getAccount(Long tenantId, Long platformUserId);

    Page<MemberPointsLog> listLogs(Long tenantId, Long platformUserId, Integer current, Integer size);

    void grantPoints(Long tenantId, Long platformUserId, Integer points, String bizType, String bizNo, String remark);

    MemberPointsLog holdPoints(Long tenantId, Long platformUserId, Integer points, String bizType, String bizNo, String remark);

    void confirmPointsHold(Long tenantId, Long platformUserId, String bizType, String bizNo);

    void releasePointsHold(Long tenantId, Long platformUserId, String bizType, String bizNo, String releaseReason);
}
