package com.payment.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("recharge_order_v1")
public class RechargeOrderV1 implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;
    private String rechargeNo;
    private String walletType;
    private Long tenantId;
    private Long platformUserId;
    private Long ruleId;
    private BigDecimal rechargeAmount;
    private BigDecimal giftAmount;
    private Integer giftPoints;
    private BigDecimal actualCreditAmount;
    private String bizStatus;
    private Integer deleted;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
