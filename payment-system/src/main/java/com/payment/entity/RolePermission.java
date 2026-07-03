package com.payment.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 角色-权限关联实体，对应数据库表 sys_role_permission。
 * <p>RBAC 五表权限模型中的中间关联表，建立角色与权限之间的多对多关系。
 * 通过此表将权限分配给角色，用户通过绑定角色间接获得权限。</p>
 */
@Data
@TableName("sys_role_permission")
public class RolePermission implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 关联记录主键ID，自增 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 角色ID，关联 sys_role 表 */
    private Long roleId;

    /** 权限ID，关联 sys_permission 表 */
    private Long permissionId;

    /** 记录创建时间 */
    private LocalDateTime createTime;
}
