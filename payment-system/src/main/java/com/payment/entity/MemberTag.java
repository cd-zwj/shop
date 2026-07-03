package com.payment.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 会员标签实体，对应数据库表 member_tag。
 * 定义租户下可用于标记会员的标签，支持手动打标、规则打标和系统自动打标。
 * 标签可用于会员分群、精准营销、消息推送等场景。
 */
@Data
@TableName("member_tag")
public class MemberTag implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 主键ID，自增 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 租户ID，多租户隔离标识 */
    private Long tenantId;

    /**
     * 标签编码，如"VIP_USER"、"FREQUENT_BUYER"，同租户下唯一
     */
    private String tagCode;

    /**
     * 标签名称，如"VIP用户"、"高频买家"，用于界面展示
     */
    private String tagName;

    /**
     * 标签类型：MANUAL-手动，RULE-规则，SYSTEM-系统
     */
    private String tagType;

    /**
     * 标签颜色，用于前端标签展示，如"#FF5722"
     */
    private String tagColor;

    /**
     * 标签描述，说明标签的用途和适用场景
     */
    private String description;

    /** 状态：1-启用，0-禁用 */
    private Integer status;

    /** 创建时间 */
    private LocalDateTime createTime;

    /** 最后更新时间 */
    private LocalDateTime updateTime;
}
