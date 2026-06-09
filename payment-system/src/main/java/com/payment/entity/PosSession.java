package com.payment.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * POS 会话实体
 */
@Data
@TableName("pos_session")
public class PosSession implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 租户ID
     */
    private Long tenantId;

    /**
     * 会话ID
     */
    private String sessionId;

    /**
     * 设备ID
     */
    private String deviceId;

    /**
     * 收银员ID
     */
    private Long cashierId;

    /**
     * 状态：ACTIVE-活跃，CLOSED-已关闭
     */
    private String status;

    /**
     * 购物车数据
     */
    private String cartData;

    /**
     * 总金额
     */
    private BigDecimal totalAmount;

    /**
     * 过期时间
     */
    private LocalDateTime expireTime;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
