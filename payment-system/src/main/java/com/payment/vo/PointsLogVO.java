package com.payment.vo;

import com.payment.dto.AssetTracePresentation;
import com.payment.dto.AssetTracePresentations;
import com.payment.entity.MemberPointsLog;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 用户端积分流水视图对象，隐藏 tenantId、platformUserId 等内部字段。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PointsLogVO {

    private Long id;
    private String bizType;
    private String bizNo;
    private Integer changePoints;
    private Integer pointsBefore;
    private Integer pointsAfter;
    private String remark;
    private String status;
    private String confirmTime;
    private String expireTime;
    private String releaseTime;
    private String releaseReason;
    private String createTime;
    private AssetTracePresentation trace;

    public static PointsLogVO from(MemberPointsLog log) {
        if (log == null) {
            return null;
        }
        return PointsLogVO.builder()
                .id(log.getId())
                .bizType(log.getBizType())
                .bizNo(log.getBizNo())
                .changePoints(log.getChangePoints())
                .pointsBefore(log.getPointsBefore())
                .pointsAfter(log.getPointsAfter())
                .remark(log.getRemark())
                .status(log.getStatus())
                .confirmTime(VoConverterUtil.formatTime(log.getConfirmTime()))
                .expireTime(VoConverterUtil.formatTime(log.getExpireTime()))
                .releaseTime(VoConverterUtil.formatTime(log.getReleaseTime()))
                .releaseReason(log.getReleaseReason())
                .createTime(VoConverterUtil.formatTime(log.getCreateTime()))
                .trace(AssetTracePresentations.points(log))
                .build();
    }
}
