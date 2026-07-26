package com.payment.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/** 售后申请的不可变处理流水。 */
@Data
@TableName("after_sale_action")
public class AfterSaleAction implements Serializable {
    private static final long serialVersionUID = 1L;
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long tenantId;
    private Long refundApplicationId;
    private String refundNo;
    private String action;
    private String operatorRole;
    private Long operatorId;
    private String remark;
    private String evidenceUrlsJson;
    private LocalDateTime createTime;
}
