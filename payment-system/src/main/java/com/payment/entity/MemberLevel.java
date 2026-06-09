package com.payment.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 会员等级实体。
 */
@Data
@TableName("member_level")
public class MemberLevel implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long tenantId;
    private String levelNo;
    private String levelName;
    private Integer levelRank;
    private Integer upgradeGrowth;
    private BigDecimal discountRate;
    private String benefitJson;
    private Integer status;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
