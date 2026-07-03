package com.payment.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.payment.common.PageResult;
import com.payment.common.Result;
import com.payment.entity.SysAuditLog;
import com.payment.mapper.SysAuditLogMapper;
import com.payment.vo.AuditLogVO;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 管理端审计日志查询接口
 */
@RestController
@RequestMapping("/v1/admin/audit-logs")
@RequiredArgsConstructor
public class V1AdminAuditLogController {

    private final SysAuditLogMapper auditLogMapper;

    @SaCheckPermission(type = "admin", value = "admin:auditlog:list")
    @GetMapping
    public Result<PageResult<AuditLogVO>> listAuditLogs(
            @RequestParam(defaultValue = "1") @Min(value = 1, message = "页码必须大于0") Integer current,
            @RequestParam(defaultValue = "10") @Min(value = 1, message = "每页条数必须大于0") Integer size,
            @RequestParam(required = false) String module,
            @RequestParam(required = false) String action,
            @RequestParam(required = false) Long operatorId) {

        Page<SysAuditLog> pageParam = new Page<>(current, size);
        LambdaQueryWrapper<SysAuditLog> wrapper = new LambdaQueryWrapper<SysAuditLog>()
                .eq(module != null && !module.isBlank(), SysAuditLog::getModule, module)
                .eq(action != null && !action.isBlank(), SysAuditLog::getAction, action)
                .eq(operatorId != null, SysAuditLog::getOperatorId, operatorId)
                .orderByDesc(SysAuditLog::getCreateTime);

        Page<SysAuditLog> page = auditLogMapper.selectPage(pageParam, wrapper);
        List<AuditLogVO> records = page.getRecords().stream()
                .map(AuditLogVO::from)
                .toList();

        return Result.success(new PageResult<>(records, page.getTotal(),
                (int) page.getCurrent(), (int) page.getSize()));
    }
}
