package com.payment.vo;

import com.payment.entity.UserNotification;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 通知视图对象，排除内部字段后返回给前端。
 */
@Data
public class NotificationVO implements Serializable {

    private static final long serialVersionUID = 1L;
    private static final Pattern ORDER_NO_PATTERN = Pattern.compile("\\b(?:SO|ORD|EX)[A-Z0-9_-]{2,}\\b");

    private Long id;
    private String title;
    private String content;
    private String category;
    private Integer readStatus;
    private LocalDateTime readTime;
    private LocalDateTime createTime;
    private String actionType;
    private String actionLabel;
    private String actionUrl;

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
        applyAction(entity, vo);
        return vo;
    }

    private static void applyAction(UserNotification entity, NotificationVO vo) {
        String category = entity.getCategory();
        String content = entity.getContent() == null ? "" : entity.getContent();
        String orderNo = extractOrderNo(content);

        if ("ORDER".equals(category) || "PAYMENT".equals(category)) {
            if (orderNo != null) {
                vo.setActionType("ORDER_DETAIL");
                vo.setActionLabel("查看订单");
                vo.setActionUrl("/order/" + encodePath(orderNo));
            }
            return;
        }

        if ("REFUND".equals(category)) {
            if (orderNo != null) {
                vo.setActionType("REFUND_DETAIL");
                vo.setActionLabel("查看售后");
                vo.setActionUrl("/orders/" + encodePath(orderNo) + "/refund");
            } else {
                vo.setActionType("ORDER_LIST");
                vo.setActionLabel("查看订单");
                vo.setActionUrl("/orders");
            }
            return;
        }

        if ("COUPON".equals(category) || "PROMOTION".equals(category)) {
            vo.setActionType("COUPON_CENTER");
            vo.setActionLabel("查看优惠券");
            vo.setActionUrl("/coupons");
        }
    }

    private static String extractOrderNo(String content) {
        Matcher matcher = ORDER_NO_PATTERN.matcher(content);
        return matcher.find() ? matcher.group() : null;
    }

    private static String encodePath(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8).replace("+", "%20");
    }
}
