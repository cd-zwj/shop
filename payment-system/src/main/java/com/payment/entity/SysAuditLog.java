package com.payment.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 运营审计日志实体。
 * 对应 sys_audit_log 表，记录平台管理端的关键操作行为。
 * 用于安全审计、操作追溯和合规检查，涵盖商户审核、订单管理、权限变更等操作。
 */
@Data
@TableName("sys_audit_log")
public class SysAuditLog implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 主键 ID，自增 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 租户ID（平台级操作可为NULL）
     */
    private Long tenantId;

    /**
     * 操作人ID
     */
    private Long operatorId;

    /**
     * 操作人类型: ADMIN/MERCHANT/USER/SYSTEM
     */
    private String operatorType;

    /**
     * 操作人名称
     */
    private String operatorName;

    /**
     * 模块名: MERCHANT/ORDER/PAYMENT等
     */
    private String module;

    /**
     * 操作类型: CREATE/UPDATE/DELETE/APPROVE/REJECT
     */
    private String action;

    /**
     * 目标类型: Tenant/Order/Withdrawal等
     */
    private String targetType;

    /**
     * 目标ID
     */
    private Long targetId;

    /**
     * 操作详情JSON
     */
    private String detail;

    /**
     * 操作IP
     */
    private String ip;

    /** 操作时间 */
    private LocalDateTime createTime;
}
