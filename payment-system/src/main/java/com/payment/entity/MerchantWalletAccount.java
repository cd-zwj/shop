package com.payment.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.Version;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("merchant_wallet_account")
public class MerchantWalletAccount implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long tenantId;
    private Long platformUserId;
    private BigDecimal availableAmount;
    private BigDecimal frozenAmount;
    private BigDecimal totalRecharge;
    private BigDecimal totalConsume;
    @Version
    private Integer version;
    private Integer status;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
