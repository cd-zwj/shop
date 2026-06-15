package com.payment.controller;

import cn.dev33.satoken.annotation.SaCheckLogin;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.payment.common.PageResult;
import com.payment.common.Result;
import com.payment.entity.UserNotification;
import com.payment.service.UserNotificationService;
import com.payment.util.PlatformSessionHelper;
import com.payment.vo.NotificationVO;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * v1 用户端通知接口。
 */
@RestController
@RequestMapping("/v1/app/notifications")
@RequiredArgsConstructor
public class V1AppNotificationController {

    private final UserNotificationService notificationService;

    @SaCheckLogin
    @GetMapping
    public Result<PageResult<NotificationVO>> listNotifications(@RequestParam(defaultValue = "1") @Min(value = 1, message = "页码必须大于0") Integer current,
                                                                @RequestParam(defaultValue = "10") @Min(value = 1, message = "每页条数必须大于0") Integer size) {
        Page<UserNotification> page = notificationService.list(PlatformSessionHelper.getPlatformUserId(), current, size);
        return Result.success(PageResult.from(page, NotificationVO::from));
    }

    @SaCheckLogin
    @GetMapping("/unread-count")
    public Result<Map<String, Long>> getUnreadCount() {
        long count = notificationService.countUnread(PlatformSessionHelper.getPlatformUserId());
        return Result.success(Map.of("count", count));
    }

    @SaCheckLogin
    @PutMapping("/{id}/read")
    public Result<NotificationVO> markRead(@PathVariable @Min(value = 1, message = "ID必须大于0") Long id) {
        UserNotification notification = notificationService.markRead(PlatformSessionHelper.getPlatformUserId(), id);
        return Result.success(NotificationVO.from(notification));
    }

    @SaCheckLogin
    @PutMapping("/read-all")
    public Result<Void> markAllRead() {
        notificationService.markAllRead(PlatformSessionHelper.getPlatformUserId());
        return Result.success();
    }
}
