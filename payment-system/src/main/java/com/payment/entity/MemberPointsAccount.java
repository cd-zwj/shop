package com.payment.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.Version;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 会员积分账户实体类，用于映射并保存会员积分账户数据。
 */
@Data
@TableName("member_points_account")
public class MemberPointsAccount implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long tenantId;
    private Long platformUserId;
    private Integer points;
    private Integer totalEarned;
    private Integer totalUsed;
    @Version
    private Integer version;
    private Integer status;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
