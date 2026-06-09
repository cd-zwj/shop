package com.payment.vo;

import com.payment.entity.UserNotification;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 通知视图对象，排除内部字段后返回给前端。
 */
@Data
public class NotificationVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;
    private String title;
    private String content;
    private String category;
    private Integer readStatus;
    private LocalDateTime readTime;
    private LocalDateTime createTime;

    public static NotificationVO from(UserNotification entity) {
        if (entity == null) {
            return null;
        }
        NotificationVO vo = new NotificationVO();
        vo.setId(entity.getId());
        vo.setTitle(entity.getTitle());
        vo.setContent(entity.getContent());
        vo.setCategory(entity.getCategory());
        vo.setReadStatus(entity.getReadStatus());
        vo.setReadTime(entity.getReadTime());
        vo.setCreateTime(entity.getCreateTime());
        return vo;
    }
}
