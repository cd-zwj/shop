package com.payment.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 会员-标签关联实体，对应数据库表 member_tag_relation。
 * 记录平台用户与标签之间的多对多关联关系，
 * 一个用户可拥有多个标签，一个标签也可被分配给多个用户。
 */
@Data
@TableName("member_tag_relation")
public class MemberAccountTag implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 主键ID，自增 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 商户ID（租户ID），多租户隔离标识
     */
    private Long tenantId;

    /**
     * 平台用户ID，关联 platform_user 表
     */
    private Long platformUserId;

    /**
     * 标签ID，关联 member_tag 表
     */
    private Long tagId;

    /**
     * 来源类型：MANUAL-手动打标，RULE-规则引擎自动打标，SYSTEM-系统自动打标
     */
    private String sourceType;

    /** 创建时间，即打标时间 */
    private LocalDateTime createTime;
}
