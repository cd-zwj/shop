package com.payment.controller;

import cn.dev33.satoken.stp.StpUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.payment.common.GlobalExceptionHandler;
import com.payment.config.TestSaTokenConfig;
import com.payment.entity.PlatformUser;
import com.payment.service.PlatformIdentityService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * V1 用户信息接口集成测试。
 * <p>
 * 验证：已登录获取用户信息、未登录访问被拒绝、返回结构正确性。
 */
@WebMvcTest(V1AppUserController.class)
@Import({TestSaTokenConfig.class, GlobalExceptionHandler.class})
@DisplayName("V1 用户信息接口集成测试")
class V1AppUserIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private PlatformIdentityService platformIdentityService;

    private PlatformUser testUser;

    @BeforeEach
    void setUp() {
        try {
            StpUtil.logout();
        } catch (Exception ignored) {
        }

        testUser = new PlatformUser();
        testUser.setId(1L);
        testUser.setUserNo("U20240101001");
        testUser.setUsername("testuser");
        testUser.setPhone("13800000000");
        testUser.setEmail("test@example.com");
        testUser.setStatus(1);
        testUser.setCreateTime(LocalDateTime.of(2024, 1, 1, 0, 0, 0));
    }

    // ===== 获取当前用户信息 =====

    @Nested
    @DisplayName("获取当前用户 GET /v1/app/users/me")
    class GetCurrentUserTests {

        @Test
        @DisplayName("已登录用户获取个人信息应返回完整用户数据")
        void getCurrentUser_已登录_返回用户信息() throws Exception {
            // 模拟登录
            StpUtil.login(1L);
            when(platformIdentityService.getCurrentUser()).thenReturn(testUser);

            mockMvc.perform(get("/v1/app/users/me")
                            .header("Authorization", StpUtil.getTokenValue()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(200))
                    .andExpect(jsonPath("$.message").value("操作成功"))
                    .andExpect(jsonPath("$.data.id").value(1))
                    .andExpect(jsonPath("$.data.userNo").value("U20240101001"))
                    .andExpect(jsonPath("$.data.username").value("testuser"))
                    .andExpect(jsonPath("$.data.phone").value("13800000000"))
                    .andExpect(jsonPath("$.data.email").value("test@example.com"))
                    .andExpect(jsonPath("$.data.status").value(1))
                    .andExpect(jsonPath("$.data.createTime").exists())
                    .andExpect(jsonPath("$.timestamp").isNumber());
        }

        @Test
        @DisplayName("未登录访问个人信息接口应返回401")
        void getCurrentUser_未登录_返回401() throws Exception {
            mockMvc.perform(get("/v1/app/users/me"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(401))
                    .andExpect(jsonPath("$.message").value("未提供Token"))
                    .andExpect(jsonPath("$.timestamp").isNumber());
        }

        @Test
        @DisplayName("使用无效Token访问应返回401")
        void getCurrentUser_无效Token_返回401() throws Exception {
            mockMvc.perform(get("/v1/app/users/me")
                            .header("Authorization", "invalid-token-xyz"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(401))
                    .andExpect(jsonPath("$.message").value("Token无效"));
        }

        @Test
        @DisplayName("返回数据不应包含密码哈希字段")
        void getCurrentUser_不泄露密码哈希() throws Exception {
            StpUtil.login(1L);
            testUser.setPasswordHash("$2a$10$secretHash");
            when(platformIdentityService.getCurrentUser()).thenReturn(testUser);

            mockMvc.perform(get("/v1/app/users/me")
                            .header("Authorization", StpUtil.getTokenValue()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.passwordHash").doesNotExist());
        }

        @Test
        @DisplayName("返回数据不应包含deleted字段")
        void getCurrentUser_不泄露内部字段() throws Exception {
            StpUtil.login(1L);
            when(platformIdentityService.getCurrentUser()).thenReturn(testUser);

            mockMvc.perform(get("/v1/app/users/me")
                            .header("Authorization", StpUtil.getTokenValue()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.deleted").doesNotExist())
                    .andExpect(jsonPath("$.data.updateTime").doesNotExist());
        }
    }
}
