package com.payment.vo;

import com.payment.entity.UserBehaviorLog;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 用户行为日志视图对象（排除 ipAddress / userAgent 等敏感字段）。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserBehaviorLogVO {

    private Long id;
    private Long tenantId;
    private Long userId;
    private String behaviorType;
    private String behaviorData;
    private String createTime;

    public static UserBehaviorLogVO from(UserBehaviorLog log) {
        if (log == null) {
            return null;
        }
        return UserBehaviorLogVO.builder()
                .id(log.getId())
                .tenantId(log.getTenantId())
                .userId(log.getUserId())
                .behaviorType(log.getBehaviorType())
                .behaviorData(log.getBehaviorData())
                .createTime(VoConverterUtil.formatTime(log.getCreateTime()))
                .build();
    }
}
