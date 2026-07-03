package com.payment.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 角色实体，对应数据库表 sys_role。
 * <p>RBAC 五表权限模型中的角色表，定义系统中的角色信息，
 * 如普通用户、商户、平台管理员等，通过角色-权限关联实现权限分配。</p>
 */
@Data
@TableName("sys_role")
public class Role implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 角色主键ID，自增 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 角色编码，全局唯一标识，如 user、merchant、admin */
    private String roleCode;

    /** 角色显示名称，用于界面展示，如"普通用户"、"平台管理员" */
    private String roleName;

    /** 角色描述信息，说明该角色的职责和适用范围 */
    private String description;

    /** 角色状态：0-禁用（该角色下所有用户失去对应权限），1-启用 */
    private Integer status;

    /** 记录创建时间 */
    private LocalDateTime createTime;

    /** 记录最后更新时间 */
    private LocalDateTime updateTime;
}
