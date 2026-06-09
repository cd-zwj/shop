package com.payment.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 积分明细实体
 */
@Data
@TableName("points_log")
public class PointsLog implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 租户ID
     */
    private Long tenantId;

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 变动类型：EARN-获得，USE-使用，EXPIRE-过期，EXCHANGE-兑换
     */
    private String changeType;

    /**
     * 变动积分
     */
    private Integer changePoints;

    /**
     * 变动前积分
     */
    private Integer pointsBefore;

    /**
     * 变动后积分
     */
    private Integer pointsAfter;

    /**
     * 关联订单号
     */
    private String orderNo;

    /**
     * 备注
     */
    private String remark;

    private LocalDateTime createTime;
}
