package com.payment.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 积分变动日志实体，对应数据库表 points_log。
 * 记录用户积分的每一笔变动明细，用于积分流水查询和对账。
 * 与 MemberPointsLog 的区别在于：本表面向通用积分场景，用户ID字段为 userId 而非 platformUserId。
 */
@Data
@TableName("points_log")
public class PointsLog implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 主键ID，自增 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 租户ID，多租户隔离标识
     */
    private Long tenantId;

    /**
     * 用户ID，关联用户表
     */
    private Long userId;

    /**
     * 变动类型：EARN-获得，USE-使用，EXPIRE-过期，EXCHANGE-兑换
     */
    private String changeType;

    /**
     * 变动积分数量，正数为获得，负数为消耗
     */
    private Integer changePoints;

    /**
     * 变动前积分余额
     */
    private Integer pointsBefore;

    /**
     * 变动后积分余额
     */
    private Integer pointsAfter;

    /**
     * 关联订单号，用于与具体业务订单关联
     */
    private String orderNo;

    /**
     * 备注说明
     */
    private String remark;

    /** 创建时间 */
    private LocalDateTime createTime;
}
