package com.payment.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 用户-权限关联实体，对应数据库表 sys_user_permission。
 */
@Data
@TableName("sys_user_permission")
public class UserPermission {
    /** 关联记录主键ID，自增 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 账号体系：admin、merchant、platform */
    private String principalType;

    /** 用户ID，必须与 principalType 共同确定唯一用户 */
    private Long userId;

    /** 权限ID，关联 sys_permission 表 */
    private Long permissionId;

    /** 记录创建时间 */
    private LocalDateTime createTime;
}
