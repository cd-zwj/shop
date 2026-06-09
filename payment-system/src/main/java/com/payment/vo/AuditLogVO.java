package com.payment.vo;

import com.payment.entity.SysAuditLog;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 审计日志视图对象。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuditLogVO {

    private Long id;
    private Long tenantId;
    private Long operatorId;
    private String operatorType;
    private String operatorName;
    private String module;
    private String action;
    private String targetType;
    private Long targetId;
    private String detail;
    private String ip;
    private String createTime;

    public static AuditLogVO from(SysAuditLog log) {
        if (log == null) {
            return null;
        }
        return AuditLogVO.builder()
                .id(log.getId())
                .tenantId(log.getTenantId())
                .operatorId(log.getOperatorId())
                .operatorType(log.getOperatorType())
                .operatorName(log.getOperatorName())
                .module(log.getModule())
                .action(log.getAction())
                .targetType(log.getTargetType())
                .targetId(log.getTargetId())
                .detail(log.getDetail())
                .ip(log.getIp())
                .createTime(VoConverterUtil.formatTime(log.getCreateTime()))
                .build();
    }
}
