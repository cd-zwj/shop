package com.payment.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.payment.entity.SysAuditLog;
import com.payment.mapper.SysAuditLogMapper;
import com.payment.service.AuditLogService;
import com.payment.vo.AuditLogVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 审计日志服务实现。
 */
@Slf4j
@Service
public class AuditLogServiceImpl
        extends ServiceImpl<SysAuditLogMapper, SysAuditLog>
        implements AuditLogService {

    @Override
    public void log(Long tenantId, Long operatorId, String operatorType, String operatorName,
                    String module, String action, String targetType, Long targetId,
                    String detail, String ip) {
        SysAuditLog entity = new SysAuditLog();
        entity.setTenantId(tenantId);
        entity.setOperatorId(operatorId);
        entity.setOperatorType(operatorType);
        entity.setOperatorName(operatorName);
        entity.setModule(module);
        entity.setAction(action);
        entity.setTargetType(targetType);
        entity.setTargetId(targetId);
        entity.setDetail(detail);
        entity.setIp(ip);
        entity.setCreateTime(LocalDateTime.now());
        baseMapper.insert(entity);
    }

    @Override
    public List<AuditLogVO> listByTenant(Long tenantId, int page, int size) {
        Page<SysAuditLog> pageParam = new Page<>(page, size);
        baseMapper.selectPage(pageParam,
                new LambdaQueryWrapper<SysAuditLog>()
                        .eq(tenantId != null, SysAuditLog::getTenantId, tenantId)
                        .orderByDesc(SysAuditLog::getCreateTime));
        return pageParam.getRecords().stream()
                .map(AuditLogVO::from)
                .toList();
    }

    @Override
    public List<AuditLogVO> listByOperator(Long operatorId, int page, int size) {
        Page<SysAuditLog> pageParam = new Page<>(page, size);
        baseMapper.selectPage(pageParam,
                new LambdaQueryWrapper<SysAuditLog>()
                        .eq(operatorId != null, SysAuditLog::getOperatorId, operatorId)
                        .orderByDesc(SysAuditLog::getCreateTime));
        return pageParam.getRecords().stream()
                .map(AuditLogVO::from)
                .toList();
    }
}
