package com.payment.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 用户行为日志实体，对应数据库表 user_behavior_log。
 * <p>记录 C 端用户在平台上的各类操作行为（浏览、搜索、支付等），用于行为分析和运营决策。</p>
 */
@Data
@TableName("user_behavior_log")
public class UserBehaviorLog implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 主键ID，自增 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 租户ID
     */
    private Long tenantId;

    /** 关联的平台用户ID */
    private Long userId;

    /**
     * 行为类型：LOGIN-登录，PAY-支付，VIEW-浏览，SEARCH-搜索
     */
    private String behaviorType;

    /** 行为附加数据，JSON 格式存储，具体内容取决于行为类型 */
    private String behaviorData;

    /** 用户操作时的客户端 IP 地址 */
    private String ipAddress;

    /** 用户操作时的浏览器 User-Agent 信息 */
    private String userAgent;

    /** 行为记录创建时间 */
    private LocalDateTime createTime;
}
