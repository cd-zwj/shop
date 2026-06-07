package com.payment.vo;

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
    private String releaseTime;
    private String releaseReason;
    private String createTime;

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
                .confirmTime(formatTime(log.getConfirmTime()))
                .releaseTime(formatTime(log.getReleaseTime()))
                .releaseReason(log.getReleaseReason())
                .createTime(formatTime(log.getCreateTime()))
                .build();
    }

    private static String formatTime(java.time.LocalDateTime time) {
        return time == null ? null : time.toString();
    }
}
