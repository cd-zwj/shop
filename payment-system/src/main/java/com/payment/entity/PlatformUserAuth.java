package com.payment.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@TableName("platform_user_auth")
public class PlatformUserAuth implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long platformUserId;
    private String authType;
    private String authKey;
    private String authUnionKey;
    private String extraJson;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
