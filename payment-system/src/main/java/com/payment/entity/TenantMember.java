package com.payment.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 租户会员实体，对应 tenant_member 表。
 * <p>记录平台用户在某个租户下的会员信息，支持会员等级、积分、成长值等功能。
 * 一个平台用户可在不同租户下拥有独立的会员身份。</p>
 */
@Data
@TableName("tenant_member")
public class TenantMember implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 主键ID，自增 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 所属租户ID，对应 tenant.id */
    private Long tenantId;

    /** 关联的平台用户ID，对应 platform_user.id */
    private Long platformUserId;

    /** 会员编号，系统生成的唯一标识，用于在租户内识别会员 */
    private String memberNo;

    /** 会员状态：0-禁用，1-正常，2-冻结 */
    private Integer memberStatus;

    /** 会员等级：数值越大等级越高，如 1-普通会员、2-银卡、3-金卡 等 */
    private Integer memberLevel;

    /** 注册来源，标识用户通过何种渠道注册成为会员，如 APP、H5、MINI_PROGRAM 等 */
    private String registerSource;

    /** 创建时间（注册时间） */
    private LocalDateTime createTime;

    /** 最后更新时间 */
    private LocalDateTime updateTime;
}
