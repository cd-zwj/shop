package com.payment.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("sys_user_permission")
public class UserPermission {
    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    private Long permissionId;

    private LocalDateTime createTime;
}
