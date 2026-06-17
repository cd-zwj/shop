package com.payment.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.payment.config.RabbitMQConfig;
import com.payment.entity.UserBehaviorLog;
import com.payment.mapper.UserBehaviorLogMapper;
import com.payment.service.OutboxPublisher;
import com.payment.service.UserBehaviorLogService;
import com.payment.service.outbox.OutboxMessageCommand;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 用户行为日志服务实现类。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserBehaviorLogServiceImpl implements UserBehaviorLogService {

    private final UserBehaviorLogMapper userBehaviorLogMapper;
    private final ObjectMapper objectMapper;
    private final OutboxPublisher outboxPublisher;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void recordBehavior(Long platformUserId,
                               Long tenantId,
                               String behaviorType,
                               String targetType,
                               Long targetId,
                               String detail) {
        UserBehaviorLog entity = new UserBehaviorLog();
        entity.setUserId(platformUserId);
        entity.setTenantId(tenantId != null ? tenantId : 0L);
        entity.setBehaviorType(behaviorType);
        entity.setBehaviorData(buildBehaviorData(targetType, targetId, detail));
        entity.setCreateTime(LocalDateTime.now());
        userBehaviorLogMapper.insert(entity);
        publishBehaviorEvent(entity, targetType, targetId, detail);
    }

    private void publishBehaviorEvent(UserBehaviorLog entity, String targetType, Long targetId, String detail) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("bizType", "USER_BEHAVIOR");
        body.put("behaviorLogId", entity.getId());
        body.put("platformUserId", entity.getUserId());
        body.put("tenantId", entity.getTenantId());
        body.put("behaviorType", entity.getBehaviorType());
        body.put("targetType", targetType);
        body.put("targetId", targetId);
        body.put("detail", detail);
        body.put("behaviorData", entity.getBehaviorData());
        body.put("createTime", entity.getCreateTime());

        outboxPublisher.publish(OutboxMessageCommand.builder()
                .messagePrefix("BHV")
                .bizType("USER_BEHAVIOR")
                .bizNo("USER_BEHAVIOR_" + entity.getId())
                .routingKey(RabbitMQConfig.USER_BEHAVIOR_QUEUE)
                .messageBody(body)
                .build());
    }

    @Override
    public Page<UserBehaviorLog> listByUser(Long platformUserId,
                                            Long tenantId,
                                            String behaviorType,
                                            int page,
                                            int size) {
        LambdaQueryWrapper<UserBehaviorLog> wrapper = new LambdaQueryWrapper<UserBehaviorLog>()
                .eq(platformUserId != null, UserBehaviorLog::getUserId, platformUserId)
                .eq(tenantId != null, UserBehaviorLog::getTenantId, tenantId)
                .eq(behaviorType != null && !behaviorType.isBlank(), UserBehaviorLog::getBehaviorType, behaviorType)
                .orderByDesc(UserBehaviorLog::getCreateTime);
        return userBehaviorLogMapper.selectPage(new Page<>(page, size), wrapper);
    }

    @Override
    public Page<UserBehaviorLog> listByTarget(String targetType,
                                              Long targetId,
                                              int page,
                                              int size) {
        LambdaQueryWrapper<UserBehaviorLog> wrapper = new LambdaQueryWrapper<UserBehaviorLog>()
                .like(targetType != null && !targetType.isBlank(), UserBehaviorLog::getBehaviorData, "\"targetType\":\"" + targetType + "\"")
                .like(targetId != null, UserBehaviorLog::getBehaviorData, "\"targetId\":" + targetId)
                .orderByDesc(UserBehaviorLog::getCreateTime);
        return userBehaviorLogMapper.selectPage(new Page<>(page, size), wrapper);
    }

    /**
     * 将 targetType / targetId / detail 合并为 behaviorData JSON 字符串。
     */
    private String buildBehaviorData(String targetType, Long targetId, String detail) {
        try {
            Map<String, Object> data = new LinkedHashMap<>();
            if (targetType != null) {
                data.put("targetType", targetType);
            }
            if (targetId != null) {
                data.put("targetId", targetId);
            }
            if (detail != null && !detail.isBlank()) {
                data.put("detail", detail);
            }
            return data.isEmpty() ? null : objectMapper.writeValueAsString(data);
        } catch (Exception e) {
            log.warn("序列化 behaviorData 失败", e);
            return detail;
        }
    }
}
