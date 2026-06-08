package com.payment.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 用户实体
 * @deprecated 使用 PlatformUser 替代
 */
@Data
@TableName("sys_user")
@Deprecated
public class User implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;
    
    /**
     * 租户ID
     */
    private Long tenantId;
    
    private String username;
    
    private String password;
    
    private String nickname;
    
    private String phone;
    
    private String email;
    
    private String avatar;
    
    /**
     * 用户类型：1-普通用户，2-管理员
     */
    private Integer userType;
    
    /**
     * 状态：0-禁用，1-启用
     */
    private Integer status;
    
    private Integer deleted;
    
    private LocalDateTime createTime;
    
    private LocalDateTime updateTime;
}

