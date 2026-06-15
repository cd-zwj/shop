package com.payment.service.impl;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.payment.common.BusinessException;
import com.payment.entity.UserNotification;
import com.payment.mapper.UserNotificationMapper;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 用户通知服务测试类，用于验证只读列表和已读状态。
 */
class UserNotificationServiceImplTest {

    /**
     * 查询通知Should只按当前用户返回未删除记录。
     */
    @Test
    void listShouldReturnCurrentUserNotifications() {
        UserNotificationMapper notificationMapper = mock(UserNotificationMapper.class);
        UserNotificationServiceImpl service = new UserNotificationServiceImpl(notificationMapper);
        Page<UserNotification> page = new Page<>(1, 20);
        page.setRecords(java.util.List.of(buildNotification(1L, 100L, 0)));
        page.setTotal(1);
        when(notificationMapper.selectPage(any(), any())).thenReturn(page);

        Page<UserNotification> result = service.list(100L, 1, 20);

        assertEquals(1, result.getRecords().size());
        assertEquals("订单状态更新", result.getRecords().get(0).getTitle());
        assertEquals(20, result.getSize());
    }

    /**
     * 标记已读Should只允许当前用户操作自己的通知。
     */
    @Test
    void markReadShouldRequireOwnership() {
        UserNotificationMapper notificationMapper = mock(UserNotificationMapper.class);
        UserNotificationServiceImpl service = new UserNotificationServiceImpl(notificationMapper);
        when(notificationMapper.selectById(1L)).thenReturn(buildNotification(1L, 200L, 0));

        assertThrows(BusinessException.class, () -> service.markRead(100L, 1L));
    }

    /**
     * 标记已读Should写入已读状态。
     */
    @Test
    void markReadShouldPersistReadStatus() {
        UserNotificationMapper notificationMapper = mock(UserNotificationMapper.class);
        UserNotificationServiceImpl service = new UserNotificationServiceImpl(notificationMapper);
        when(notificationMapper.selectById(1L)).thenReturn(buildNotification(1L, 100L, 0));

        UserNotification result = service.markRead(100L, 1L);

        ArgumentCaptor<UserNotification> captor = ArgumentCaptor.forClass(UserNotification.class);
        verify(notificationMapper).updateById(captor.capture());
        assertEquals(1, captor.getValue().getReadStatus());
        assertEquals(1, result.getReadStatus());
    }

    /**
     * 已读通知再次标记Should直接返回并避免重复写库。
     */
    @Test
    void markReadShouldSkipPersistWhenAlreadyRead() {
        UserNotificationMapper notificationMapper = mock(UserNotificationMapper.class);
        UserNotificationServiceImpl service = new UserNotificationServiceImpl(notificationMapper);
        when(notificationMapper.selectById(1L)).thenReturn(buildNotification(1L, 100L, 1));

        UserNotification result = service.markRead(100L, 1L);

        verify(notificationMapper, never()).updateById(any());
        assertEquals(1, result.getReadStatus());
    }

    /**
     * 统计未读Should返回 mapper 计数，null Should兜底为0。
     */
    @Test
    void countUnreadShouldReturnMapperCount() {
        UserNotificationMapper notificationMapper = mock(UserNotificationMapper.class);
        UserNotificationServiceImpl service = new UserNotificationServiceImpl(notificationMapper);
        when(notificationMapper.selectCount(any())).thenReturn(3L);

        assertEquals(3L, service.countUnread(100L));
    }

    @Test
    void countUnreadShouldReturnZeroWhenMapperReturnsNull() {
        UserNotificationMapper notificationMapper = mock(UserNotificationMapper.class);
        UserNotificationServiceImpl service = new UserNotificationServiceImpl(notificationMapper);
        when(notificationMapper.selectCount(any())).thenReturn(null);

        assertEquals(0L, service.countUnread(100L));
    }

    /**
     * 全部已读Should批量更新并写入已读时间，返回受影响条数。
     */
    @Test
    void markAllReadShouldBatchUpdateUnread() {
        UserNotificationMapper notificationMapper = mock(UserNotificationMapper.class);
        UserNotificationServiceImpl service = new UserNotificationServiceImpl(notificationMapper);
        when(notificationMapper.update(any(), any())).thenReturn(5);

        int affected = service.markAllRead(100L);

        ArgumentCaptor<UserNotification> captor = ArgumentCaptor.forClass(UserNotification.class);
        verify(notificationMapper).update(captor.capture(), any());
        assertEquals(1, captor.getValue().getReadStatus());
        assertEquals(5, affected);
    }

    private UserNotification buildNotification(Long id, Long platformUserId, Integer readStatus) {
        UserNotification notification = new UserNotification();
        notification.setId(id);
        notification.setPlatformUserId(platformUserId);
        notification.setTitle("订单状态更新");
        notification.setContent("你的订单已完成。");
        notification.setCategory("ORDER");
        notification.setReadStatus(readStatus);
        notification.setDeleted(0);
        return notification;
    }
}
