package com.payment.service.impl;

import com.payment.common.BusinessException;
import com.payment.entity.UserNotification;
import com.payment.mapper.UserNotificationMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * 单元测试 — UserNotificationServiceImpl.send() 方法。
 * 使用 Mockito 隔离 Mapper 层，纯测 Service 逻辑。
 */
@ExtendWith(MockitoExtension.class)
class UserNotificationServiceImplSendTest {

    @Mock
    private UserNotificationMapper notificationMapper;

    @InjectMocks
    private UserNotificationServiceImpl notificationService;

    // ========== 正常路径 ==========

    @Test
    void send_shouldCreateNotification_whenAllParamsValid() {
        // Arrange
        when(notificationMapper.insert(any(UserNotification.class))).thenReturn(1);

        // Act
        UserNotification result = notificationService.send(1001L, "订单已发货", "您的订单 ORD-001 已发货，请注意查收。", "ORDER");

        // Assert
        assertNotNull(result);
        assertEquals(1001L, result.getPlatformUserId());
        assertEquals("订单已发货", result.getTitle());
        assertEquals("您的订单 ORD-001 已发货，请注意查收。", result.getContent());
        assertEquals("ORDER", result.getCategory());
        assertEquals(0, result.getReadStatus());
        assertEquals(0, result.getDeleted());
        assertNotNull(result.getCreateTime());
        assertNotNull(result.getUpdateTime());
        verify(notificationMapper, times(1)).insert(any(UserNotification.class));
    }

    @ParameterizedTest
    @ValueSource(strings = {"ORDER", "PAYMENT", "REFUND", "COUPON", "SYSTEM", "PROMOTION"})
    void send_shouldSucceed_forAllAllowedCategories(String category) {
        // Arrange
        when(notificationMapper.insert(any(UserNotification.class))).thenReturn(1);

        // Act
        UserNotification result = notificationService.send(1L, "标题", "内容", category);

        // Assert
        assertNotNull(result);
        assertEquals(category, result.getCategory());
    }

    // ========== platformUserId 校验 ==========

    @Test
    void send_shouldThrow_whenPlatformUserIdIsNull() {
        // Arrange & Act & Assert
        BusinessException ex = assertThrows(BusinessException.class,
                () -> notificationService.send(null, "标题", "内容", "ORDER"));
        assertEquals("通知目标用户ID不合法", ex.getMessage());
        verifyNoInteractions(notificationMapper);
    }

    @Test
    void send_shouldThrow_whenPlatformUserIdIsZero() {
        // Arrange & Act & Assert
        BusinessException ex = assertThrows(BusinessException.class,
                () -> notificationService.send(0L, "标题", "内容", "ORDER"));
        assertEquals("通知目标用户ID不合法", ex.getMessage());
        verifyNoInteractions(notificationMapper);
    }

    @Test
    void send_shouldThrow_whenPlatformUserIdIsNegative() {
        // Arrange & Act & Assert
        BusinessException ex = assertThrows(BusinessException.class,
                () -> notificationService.send(-1L, "标题", "内容", "ORDER"));
        assertEquals("通知目标用户ID不合法", ex.getMessage());
        verifyNoInteractions(notificationMapper);
    }

    // ========== title 校验 ==========

    @Test
    void send_shouldThrow_whenTitleIsNull() {
        // Arrange & Act & Assert
        BusinessException ex = assertThrows(BusinessException.class,
                () -> notificationService.send(1L, null, "内容", "ORDER"));
        assertEquals("通知标题不能为空且不超过200字", ex.getMessage());
    }

    @Test
    void send_shouldThrow_whenTitleIsEmpty() {
        // Arrange & Act & Assert
        BusinessException ex = assertThrows(BusinessException.class,
                () -> notificationService.send(1L, "", "内容", "ORDER"));
        assertEquals("通知标题不能为空且不超过200字", ex.getMessage());
    }

    @Test
    void send_shouldThrow_whenTitleIsBlank() {
        // Arrange & Act & Assert
        BusinessException ex = assertThrows(BusinessException.class,
                () -> notificationService.send(1L, "   ", "内容", "ORDER"));
        assertEquals("通知标题不能为空且不超过200字", ex.getMessage());
    }

    @Test
    void send_shouldThrow_whenTitleExceeds200Chars() {
        // Arrange
        String longTitle = "标".repeat(201);

        // Act & Assert
        BusinessException ex = assertThrows(BusinessException.class,
                () -> notificationService.send(1L, longTitle, "内容", "ORDER"));
        assertEquals("通知标题不能为空且不超过200字", ex.getMessage());
    }

    @Test
    void send_shouldSucceed_whenTitleIsExactly200Chars() {
        // Arrange
        String title200 = "标".repeat(200);
        when(notificationMapper.insert(any(UserNotification.class))).thenReturn(1);

        // Act
        UserNotification result = notificationService.send(1L, title200, "内容", "ORDER");

        // Assert
        assertEquals(title200, result.getTitle());
    }

    // ========== content 校验 ==========

    @Test
    void send_shouldThrow_whenContentIsNull() {
        // Arrange & Act & Assert
        BusinessException ex = assertThrows(BusinessException.class,
                () -> notificationService.send(1L, "标题", null, "ORDER"));
        assertEquals("通知内容不能为空且不超过5000字", ex.getMessage());
    }

    @Test
    void send_shouldThrow_whenContentIsEmpty() {
        // Arrange & Act & Assert
        BusinessException ex = assertThrows(BusinessException.class,
                () -> notificationService.send(1L, "标题", "", "ORDER"));
        assertEquals("通知内容不能为空且不超过5000字", ex.getMessage());
    }

    @Test
    void send_shouldThrow_whenContentIsBlank() {
        // Arrange & Act & Assert
        BusinessException ex = assertThrows(BusinessException.class,
                () -> notificationService.send(1L, "标题", "   ", "ORDER"));
        assertEquals("通知内容不能为空且不超过5000字", ex.getMessage());
    }

    @Test
    void send_shouldThrow_whenContentExceeds5000Chars() {
        // Arrange
        String longContent = "内".repeat(5001);

        // Act & Assert
        BusinessException ex = assertThrows(BusinessException.class,
                () -> notificationService.send(1L, "标题", longContent, "ORDER"));
        assertEquals("通知内容不能为空且不超过5000字", ex.getMessage());
    }

    @Test
    void send_shouldSucceed_whenContentIsExactly5000Chars() {
        // Arrange
        String content5000 = "内".repeat(5000);
        when(notificationMapper.insert(any(UserNotification.class))).thenReturn(1);

        // Act
        UserNotification result = notificationService.send(1L, "标题", content5000, "ORDER");

        // Assert
        assertEquals(content5000, result.getContent());
    }

    // ========== category 校验 ==========

    @Test
    void send_shouldThrow_whenCategoryIsNull() {
        // Arrange & Act & Assert
        BusinessException ex = assertThrows(BusinessException.class,
                () -> notificationService.send(1L, "标题", "内容", null));
        assertTrue(ex.getMessage().contains("通知分类不合法"));
    }

    @Test
    void send_shouldThrow_whenCategoryIsEmpty() {
        // Arrange & Act & Assert
        BusinessException ex = assertThrows(BusinessException.class,
                () -> notificationService.send(1L, "标题", "内容", ""));
        assertTrue(ex.getMessage().contains("通知分类不合法"));
    }

    @Test
    void send_shouldThrow_whenCategoryIsInvalid() {
        // Arrange & Act & Assert
        BusinessException ex = assertThrows(BusinessException.class,
                () -> notificationService.send(1L, "标题", "内容", "INVALID"));
        assertTrue(ex.getMessage().contains("通知分类不合法"));
    }

    @Test
    void send_shouldThrow_whenCategoryIsLowercaseValid() {
        // Arrange — 大小写敏感："order" 不等于 "ORDER"
        BusinessException ex = assertThrows(BusinessException.class,
                () -> notificationService.send(1L, "标题", "内容", "order"));
        assertTrue(ex.getMessage().contains("通知分类不合法"));
    }

    // ========== XSS 防御 ==========

    @Test
    void send_shouldStripHtmlTags_fromTitle() {
        // Arrange
        when(notificationMapper.insert(any(UserNotification.class))).thenReturn(1);

        // Act
        UserNotification result = notificationService.send(1L,
                "<script>alert('xss')</script>安全标题",
                "正常内容", "ORDER");

        // Assert — <script>alert('xss')</script> 被去除，仅保留 "安全标题"
        assertEquals("alert('xss')安全标题", result.getTitle());
    }

    @Test
    void send_shouldStripHtmlTags_fromContent() {
        // Arrange
        when(notificationMapper.insert(any(UserNotification.class))).thenReturn(1);

        // Act
        UserNotification result = notificationService.send(1L,
                "标题",
                "<b>加粗</b><a href='http://evil.com'>点击</a>正文",
                "ORDER");

        // Assert — 所有 HTML 标签被去除
        assertEquals("加粗点击正文", result.getContent());
    }

    @Test
    void send_shouldHandleNestedHtmlTags() {
        // Arrange
        when(notificationMapper.insert(any(UserNotification.class))).thenReturn(1);

        // Act
        UserNotification result = notificationService.send(1L,
                "<div><span><b>深层嵌套</b></span></div>",
                "内容", "ORDER");

        // Assert
        assertEquals("深层嵌套", result.getTitle());
    }

    @Test
    void send_shouldPreserveText_withoutHtml() {
        // Arrange
        when(notificationMapper.insert(any(UserNotification.class))).thenReturn(1);

        // Act
        UserNotification result = notificationService.send(1L,
                "纯文本标题无HTML",
                "纯文本内容无HTML",
                "PAYMENT");

        // Assert
        assertEquals("纯文本标题无HTML", result.getTitle());
        assertEquals("纯文本内容无HTML", result.getContent());
    }

    // ========== readStatus & deleted 默认值 ==========

    @Test
    void send_shouldSetReadStatusToZero() {
        // Arrange
        when(notificationMapper.insert(any(UserNotification.class))).thenReturn(1);

        // Act
        UserNotification result = notificationService.send(1L, "标题", "内容", "ORDER");

        // Assert
        assertEquals(0, result.getReadStatus());
    }

    @Test
    void send_shouldSetDeletedToZero() {
        // Arrange
        when(notificationMapper.insert(any(UserNotification.class))).thenReturn(1);

        // Act
        UserNotification result = notificationService.send(1L, "标题", "内容", "ORDER");

        // Assert
        assertEquals(0, result.getDeleted());
    }

    // ========== insert 交互验证 ==========

    @Test
    void send_shouldCallMapperInsertExactlyOnce() {
        // Arrange
        when(notificationMapper.insert(any(UserNotification.class))).thenReturn(1);

        // Act
        notificationService.send(42L, "通知标题", "通知内容", "SYSTEM");

        // Assert
        ArgumentCaptor<UserNotification> captor = ArgumentCaptor.forClass(UserNotification.class);
        verify(notificationMapper, times(1)).insert(captor.capture());
        UserNotification captured = captor.getValue();
        assertEquals(42L, captured.getPlatformUserId());
        assertEquals("SYSTEM", captured.getCategory());
    }
}
