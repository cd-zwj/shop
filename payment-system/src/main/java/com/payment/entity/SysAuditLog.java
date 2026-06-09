package com.payment.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 运营审计日志实体
 */
@Data
@TableName("sys_audit_log")
public class SysAuditLog implements Serializable {
    private static final long serialVersionUID = 1L;

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

    private LocalDateTime createTime;
}
