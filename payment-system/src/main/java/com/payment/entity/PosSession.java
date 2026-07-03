package com.payment.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * POS终端会话实体，对应数据库表 pos_session。
 * <p>记录收银员在POS终端设备上的会话状态，包括当前购物车数据、
 * 总金额和会话过期时间等。用于线下门店收银场景，支持会话恢复。</p>
 */
@Data
@TableName("pos_session")
public class PosSession implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 会话记录主键ID，自增 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 租户ID，用于多租户行级隔离 */
    private Long tenantId;

    /** 会话唯一标识，UUID格式，用于前端/设备端追踪会话 */
    private String sessionId;

    /** POS终端设备唯一标识，用于关联具体硬件设备 */
    private String deviceId;

    /** 收银员用户ID，关联 sys_user 表 */
    private Long cashierId;

    /** 会话状态：ACTIVE-活跃（正在收银），CLOSED-已关闭（结算完成或超时） */
    private String status;

    /** 购物车数据，JSON格式存储当前会话中待结算的商品列表 */
    private String cartData;

    /** 购物车商品总金额，单位为元，精度到分 */
    private BigDecimal totalAmount;

    /** 会话过期时间，超时后自动关闭，防止僵尸会话占用资源 */
    private LocalDateTime expireTime;

    /** 记录创建时间 */
    private LocalDateTime createTime;

    /** 记录最后更新时间 */
    private LocalDateTime updateTime;
}
