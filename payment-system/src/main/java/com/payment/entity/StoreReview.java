package com.payment.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/** 已完成自提订单的门店评价。 */
@Data
@TableName("store_review")
public class StoreReview implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long tenantId;
    private Long storeId;
    private Long orderId;
    private String orderNo;
    private Long platformUserId;
    private Integer rating;
    private String content;
    private String imageUrlsJson;
    private String merchantReply;
    private Long merchantReplyOperatorId;
    private LocalDateTime merchantReplyTime;
    private String status;
    private String moderationRemark;
    private Long moderationOperatorId;
    private LocalDateTime moderationTime;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
