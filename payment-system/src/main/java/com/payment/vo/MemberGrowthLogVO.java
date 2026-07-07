package com.payment.vo;

import com.payment.dto.AssetTracePresentation;
import com.payment.dto.AssetTracePresentations;
import com.payment.entity.MemberGrowthLog;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 用户端成长值日志视图对象，隐藏 tenantId、platformUserId 等内部字段。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MemberGrowthLogVO {

    private Long id;
    private String changeType;
    private Integer changeGrowth;
    private Integer growthBefore;
    private Integer growthAfter;
    private String bizType;
    private String bizNo;
    private String remark;
    private String createTime;
    private AssetTracePresentation trace;

    public static MemberGrowthLogVO from(MemberGrowthLog log) {
        if (log == null) {
            return null;
        }
        return MemberGrowthLogVO.builder()
                .id(log.getId())
                .changeType(log.getChangeType())
                .changeGrowth(log.getChangeGrowth())
                .growthBefore(log.getGrowthBefore())
                .growthAfter(log.getGrowthAfter())
                .bizType(log.getBizType())
                .bizNo(log.getBizNo())
                .remark(log.getRemark())
                .createTime(VoConverterUtil.formatTime(log.getCreateTime()))
                .trace(AssetTracePresentations.growth(log))
                .build();
    }
}
