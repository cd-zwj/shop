package com.payment.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 用户通知实体类，对应数据库表 user_notification。
 * <p>用于保存和管理 C 端用户收到的各类站内通知，包括系统公告、订单提醒、营销推送等。</p>
 */
@Data
@TableName("user_notification")
public class UserNotification implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 主键ID，自增 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 关联的平台用户ID */
    private Long platformUserId;

    /** 通知标题 */
    private String title;

    /** 通知正文内容，支持富文本或纯文本 */
    private String content;

    /** 通知分类：ORDER-订单通知，SYSTEM-系统公告，MARKETING-营销推送，WALLET-钱包变动等 */
    private String category;

    /** 阅读状态：0-未读，1-已读 */
    private Integer readStatus;

    /** 软删除标记：0-正常，1-已删除（用户侧不可见） */
    private Integer deleted;

    /** 用户阅读通知的时间，未读时为 null */
    private LocalDateTime readTime;

    /** 通知创建时间 */
    private LocalDateTime createTime;

    /** 记录最后更新时间 */
    private LocalDateTime updateTime;
}
