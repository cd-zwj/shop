package com.payment.vo;

import com.payment.entity.MemberPointsAccount;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 用户端积分账户视图对象，隐藏 tenantId、platformUserId、version 等内部字段。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PointsAccountVO {

    private Long id;
    private Integer points;
    private Integer totalEarned;
    private Integer totalUsed;
    private Integer expiringSoonPoints;
    private Integer status;
    private String createTime;
    private String updateTime;

    public static PointsAccountVO from(MemberPointsAccount account) {
        if (account == null) {
            return null;
        }
        return PointsAccountVO.builder()
                .id(account.getId())
                .points(account.getPoints())
                .totalEarned(account.getTotalEarned())
                .totalUsed(account.getTotalUsed())
                .expiringSoonPoints(0)
                .status(account.getStatus())
                .createTime(VoConverterUtil.formatTime(account.getCreateTime()))
                .updateTime(VoConverterUtil.formatTime(account.getUpdateTime()))
                .build();
    }

    public static PointsAccountVO from(MemberPointsAccount account, Integer expiringSoonPoints) {
        PointsAccountVO vo = from(account);
        if (vo != null) {
            vo.setExpiringSoonPoints(expiringSoonPoints == null ? 0 : expiringSoonPoints);
        }
        return vo;
    }
}
