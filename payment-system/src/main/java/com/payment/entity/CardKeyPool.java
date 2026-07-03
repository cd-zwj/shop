package com.payment.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 卡密库存池实体，对应数据库表 card_key_pool。
 * <p>用于管理虚拟商品（如充值卡、礼品卡、激活码等）的卡密库存。
 * 商户批量上传卡密后入库，用户下单时自动锁定一条可用卡密，
 * 订单完成时标记为已使用，退款时可回退卡密状态。</p>
 * <p>状态流转：AVAILABLE -> LOCKED -> USED -> (RETURNED)</p>
 */
@Data
@TableName("card_key_pool")
public class CardKeyPool implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 卡密记录主键ID，自增 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 租户ID，用于多租户数据隔离 */
    private Long tenantId;

    /** 关联的商品ID，对应 product 表主键，product_type 须为 CARD_KEY */
    private Long productId;

    /** 卡密内容（如充值卡号+密码、激活码等） */
    private String cardCode;

    /** 卡密状态：AVAILABLE-可用 / LOCKED-已锁定（待支付） / USED-已使用 */
    private String status;

    /** 关联的订单号，卡密被锁定或使用时记录 */
    private String orderNo;

    /** 关联的订单明细ID，定位到具体购买项 */
    private Long orderItemId;

    /** 卡密被使用（交付成功）的时间 */
    private LocalDateTime usedTime;

    /** 卡密被退回的时间（如退款后回退） */
    private LocalDateTime returnedTime;

    /** 退回原因，卡密状态变为 RETURNED 时记录 */
    private String returnReason;

    /** 逻辑删除标记：0-未删除，1-已删除（MyBatis-Plus @TableLogic） */
    @TableLogic
    private Integer deleted;

    /** 创建时间（卡密入库时间） */
    private LocalDateTime createTime;

    /** 最后更新时间 */
    private LocalDateTime updateTime;
}
