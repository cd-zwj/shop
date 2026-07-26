package com.payment.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 租户员工实体，对应 tenant_employee 表。
 * <p>记录租户（商户）下的员工信息，将平台用户与租户关联起来，
 * 并赋予员工在该租户内的角色（如店长、店员等）。
 * 一个平台用户可作为员工加入多个租户。</p>
 */
@Data
@TableName("tenant_employee")
public class TenantEmployee implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 主键ID，自增 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 所属租户ID，对应 tenant.id */
    private Long tenantId;

    /** 关联的平台用户ID，对应 platform_user.id */
    private Long platformUserId;

    /** 员工编号，系统生成的唯一标识，用于在租户内区分员工 */
    private String employeeNo;

    /** 员工角色，如 OWNER-店主、MANAGER-店长、PICKUP_CLERK-自提店员 等 */
    private String employeeRole;

    /** 员工状态：0-禁用，1-正常；禁用后该员工无法访问租户后台 */
    private Integer status;

    /** 创建时间（加入时间） */
    private LocalDateTime createTime;

    /** 最后更新时间 */
    private LocalDateTime updateTime;
}
