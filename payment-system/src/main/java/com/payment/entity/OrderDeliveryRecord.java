package com.payment.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 订单交付记录。
 *
 * 所有商品类型共用一张表，{@code payload} 按 {@code productType} 解读：
 * <ul>
 *   <li>VIRTUAL：{"contentUrl":"...","accountInfo":"..."}</li>
 *   <li>CARD_KEY：{"cardKeyId":1,"code":"XXXX-XXXX","placeholder":false}</li>
 *   <li>SERVICE：{"verifyCode":"123456","placeholder":false}</li>
 *   <li>SUBSCRIPTION：{"validityDays":30}（expireTime 字段另存）</li>
 *   <li>PHYSICAL：{"shippingNo":"SF1234","logisticsCompany":"顺丰"}</li>
 * </ul>
 */
@Data
@TableName("order_delivery_record")
public class OrderDeliveryRecord implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long tenantId;
    private Long orderId;
    private String orderNo;
    private Long orderItemId;
    private Long platformUserId;
    private Long productId;

    /** 商品类型，决定 payload 的解读方式 */
    private String productType;

    /** 状态：PENDING / DELIVERED / CONFIRMED / REVOKED / FAILED */
    private String status;

    /** JSON 交付内容，按 productType 解读 */
    private String payload;

    private String failReason;
    private Integer retryCount;
    private LocalDateTime deliveredTime;
    private LocalDateTime confirmedTime;
    private LocalDateTime expireTime;
    private LocalDateTime revokedTime;

    @TableLogic
    private Integer deleted;

    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
