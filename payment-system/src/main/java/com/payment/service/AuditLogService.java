package com.payment.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.payment.entity.SysAuditLog;
import com.payment.vo.AuditLogVO;

import java.util.List;

/**
 * 审计日志服务接口
 */
public interface AuditLogService extends IService<SysAuditLog> {

    /**
     * 记录一条审计日志
     */
    void log(Long tenantId, Long operatorId, String operatorType, String operatorName,
             String module, String action, String targetType, Long targetId,
             String detail, String ip);

    /**
     * 按租户分页查询审计日志
     */
    List<AuditLogVO> listByTenant(Long tenantId, int page, int size);

    /**
     * 按操作人分页查询审计日志
     */
    List<AuditLogVO> listByOperator(Long operatorId, int page, int size);
}
