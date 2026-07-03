package com.payment.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 卡密交付数据传输对象，用于虚拟商品（如充值卡、激活码）的发货交付。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CardKeyDeliveryDTO {
    /** 卡密 ID */
    private Long cardKeyId;
    /** 卡密码/激活码 */
    private String code;
}
