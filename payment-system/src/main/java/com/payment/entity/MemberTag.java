package com.payment.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 会员标签实体。
 */
@Data
@TableName("member_tag")
public class MemberTag implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long tenantId;
    /**
     * 标签编码
     */
    private String tagCode;
    /**
     * 标签名称
     */
    private String tagName;
    /**
     * 标签类型：MANUAL-手动，RULE-规则，SYSTEM-系统
     */
    private String tagType;
    /**
     * 标签颜色
     */
    private String tagColor;
    /**
     * 标签描述
     */
    private String description;
    private Integer status;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
