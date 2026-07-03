package com.payment.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 权限实体，对应数据库表 sys_permission。
 * <p>RBAC 五表权限模型中的权限表，定义系统中所有可分配的细粒度权限，
 * 如"创建订单"、"查看报表"、"管理商品"等，通过角色-权限关联绑定到角色。</p>
 */
@Data
@TableName("sys_permission")
public class Permission implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 权限主键ID，自增 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 权限编码，全局唯一标识，如 order:create、product:manage，采用 模块:操作 格式 */
    private String permissionCode;

    /** 权限显示名称，用于界面展示，如"创建订单"、"管理商品" */
    private String permissionName;

    /** 所属业务模块，用于权限分组展示，如"订单管理"、"商品管理"、"财务管理" */
    private String module;

    /** 权限描述信息，说明该权限控制的具体操作范围 */
    private String description;

    /** 记录创建时间 */
    private LocalDateTime createTime;
}
