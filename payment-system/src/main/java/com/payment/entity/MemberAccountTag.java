package com.payment.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 会员标签关联实体（member_tag_relation 表）。
 */
@Data
@TableName("member_tag_relation")
public class MemberAccountTag implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;
    /**
     * 商户ID
     */
    private Long tenantId;
    /**
     * 平台用户ID
     */
    private Long platformUserId;
    /**
     * 标签ID
     */
    private Long tagId;
    /**
     * 来源：MANUAL-手动，RULE-规则，SYSTEM-系统
     */
    private String sourceType;
    private LocalDateTime createTime;
}
