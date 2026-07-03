package com.payment.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 用户-角色关联实体，对应 sys_user_role 表。
 */
@Data
@TableName("sys_user_role")
public class UserRole implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 主键ID，自增 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 账号体系：admin、merchant、platform */
    private String principalType;

    /** 用户ID，必须与 principalType 共同确定唯一用户 */
    private Long userId;

    /** 角色ID，关联 sys_role.id */
    private Long roleId;

    /** 创建时间（授权时间） */
    private LocalDateTime createTime;
}
