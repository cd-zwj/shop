package com.payment.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.payment.entity.SysAuditLog;
import com.payment.vo.AuditLogVO;

import java.util.List;

/**
 * 审计日志服务接口。
 * <p>
 * 记录平台关键操作的审计日志，支持按租户和操作人查询。
 * 审计日志用于安全追溯、合规审查和问题排查。
 */
public interface AuditLogService extends IService<SysAuditLog> {

    /**
     * 记录一条审计日志。
     *
     * @param tenantId     租户 ID
     * @param operatorId   操作人 ID
     * @param operatorType 操作人类型（如 ADMIN、MERCHANT、USER）
     * @param operatorName 操作人名称
     * @param module       操作模块（如 USER_MANAGEMENT、ORDER 等）
     * @param action       操作动作（如 CREATE、UPDATE、DELETE 等）
     * @param targetType   操作对象类型（如 User、Order 等）
     * @param targetId     操作对象 ID
     * @param detail       操作详情描述
     * @param ip           操作人 IP 地址
     */
    void log(Long tenantId, Long operatorId, String operatorType, String operatorName,
             String module, String action, String targetType, Long targetId,
             String detail, String ip);

    /**
     * 按租户分页查询审计日志。
     *
     * @param tenantId 租户 ID
     * @param page     页码
     * @param size     每页数量
     * @return 审计日志视图列表
     */
    List<AuditLogVO> listByTenant(Long tenantId, int page, int size);

    /**
     * 按操作人分页查询审计日志。
     *
     * @param operatorId 操作人 ID
     * @param page       页码
     * @param size       每页数量
     * @return 审计日志视图列表
     */
    List<AuditLogVO> listByOperator(Long operatorId, int page, int size);
}
