package com.payment.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.payment.entity.SysAuditLog;
import org.apache.ibatis.annotations.Mapper;

/**
 * 系统审计日志数据访问接口，提供系统审计日志表（sys_audit_log）的 CRUD 操作。
 * 记录关键业务操作（如资金变动、权限变更）的审计轨迹，满足合规要求。
 */
@Mapper
public interface SysAuditLogMapper extends BaseMapper<SysAuditLog> {
}
