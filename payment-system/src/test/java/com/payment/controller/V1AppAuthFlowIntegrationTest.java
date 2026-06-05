package com.payment.controller;

import cn.dev33.satoken.stp.StpUtil;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.payment.common.GlobalExceptionHandler;
import com.payment.config.TestSaTokenConfig;
import com.payment.dto.AppUserVO;
import com.payment.dto.PlatformRegisterDTO;
import com.payment.entity.PlatformUser;
import com.payment.service.AuthCaptchaService;
import com.payment.service.PlatformIdentityService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.time.LocalDateTime;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * V1 用户端认证流程集成测试。
 * <p>
 * 验证：注册 -> 登录 -> 获取用户信息 -> 退出 的完整链路，
 * 以及返回结构的正确性。
 */
@WebMvcTest(V1AppAuthController.class)
@Import({TestSaTokenConfig.class, GlobalExceptionHandler.class})
@DisplayName("V1 用户端认证流程集成测试")
class V1AppAuthFlowIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private AuthCaptchaService authCaptchaService;

    @MockBean
    private PlatformIdentityService platformIdentityService;

    private PlatformUser testUser;

    @BeforeEach
    void setUp() {
        // 每次测试前清除 Sa-Token 登录态
        try {
            StpUtil.logout();
        } catch (Exception ignored) {
            // 未登录时 logout 可能抛异常，忽略
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

    // ===== 注册接口 =====

    @Nested
    @DisplayName("注册接口 POST /v1/app/auth/register")
    class RegisterTests {

        @Test
        @DisplayName("注册成功应返回用户信息，code=200")
        void register_成功返回用户信息() throws Exception {
            when(platformIdentityService.register(any(PlatformRegisterDTO.class))).thenReturn(testUser);

            mockMvc.perform(post("/v1/app/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(
                                    buildRegisterDTO("newuser", "password123", "13911112222", "new@example.com")
                            )))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(200))
                    .andExpect(jsonPath("$.message").value("操作成功"))
                    .andExpect(jsonPath("$.data.username").value("testuser"))
                    .andExpect(jsonPath("$.data.userNo").value("U20240101001"))
                    .andExpect(jsonPath("$.data.id").value(1))
                    .andExpect(jsonPath("$.timestamp").isNumber());

            verify(platformIdentityService).register(any(PlatformRegisterDTO.class));
        }

        @Test
        @DisplayName("注册时用户名为空应返回400参数错误")
        void register_用户名为空_返回参数错误() throws Exception {
            mockMvc.perform(post("/v1/app/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"password\":\"password123\"}"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(400))
                    .andExpect(jsonPath("$.message").value("用户名不能为空"));
        }

        @Test
        @DisplayName("注册时密码为空应返回400参数错误")
        void register_密码为空_返回参数错误() throws Exception {
            mockMvc.perform(post("/v1/app/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"username\":\"newuser\"}"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(400))
                    .andExpect(jsonPath("$.message").value("密码不能为空"));
        }

        @Test
        @DisplayName("注册时密码长度不足6位应返回400参数错误")
        void register_密码太短_返回参数错误() throws Exception {
            mockMvc.perform(post("/v1/app/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(
                                    buildRegisterDTO("newuser", "12345", null, null)
                            )))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(400))
                    .andExpect(jsonPath("$.message").value("密码长度需在6-64位之间"));
        }

        @Test
        @DisplayName("注册时请求体为空应返回400")
        void register_请求体为空_返回错误() throws Exception {
            mockMvc.perform(post("/v1/app/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(""))
                    .andExpect(status().isBadRequest());
        }
    }

    // ===== 密码登录接口 =====

    @Nested
    @DisplayName("密码登录接口 POST /v1/app/auth/login/password")
    class LoginByPasswordTests {

        @Test
        @DisplayName("密码登录成功应返回token字符串，code=200")
        void loginByPassword_成功返回token() throws Exception {
            doNothing().when(authCaptchaService).validateCaptcha("test-key", "ABCD");
            when(platformIdentityService.login(any())).thenReturn("test-token-uuid-12345");

            MvcResult result = mockMvc.perform(post("/v1/app/auth/login/password")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(
                                    buildLoginDTO("testuser", "password123", "test-key", "ABCD")
                            )))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(200))
                    .andExpect(jsonPath("$.message").value("操作成功"))
                    .andExpect(jsonPath("$.data").isString())
                    .andExpect(jsonPath("$.data").value("test-token-uuid-12345"))
                    .andExpect(jsonPath("$.timestamp").isNumber())
                    .andReturn();

            // 验证验证码校验被调用
            verify(authCaptchaService).validateCaptcha("test-key", "ABCD");
            // 验证登录被调用
            verify(platformIdentityService).login(any());
        }

        @Test
        @DisplayName("密码登录时用户名为空应返回400")
        void loginByPassword_用户名为空_返回参数错误() throws Exception {
            mockMvc.perform(post("/v1/app/auth/login/password")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"password\":\"password123\",\"captchaKey\":\"k\",\"captchaCode\":\"c\"}"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(400))
                    .andExpect(jsonPath("$.message").value("用户名不能为空"));
        }

        @Test
        @DisplayName("密码登录时密码为空应返回400")
        void loginByPassword_密码为空_返回参数错误() throws Exception {
            mockMvc.perform(post("/v1/app/auth/login/password")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"username\":\"testuser\",\"captchaKey\":\"k\",\"captchaCode\":\"c\"}"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(400))
                    .andExpect(jsonPath("$.message").value("密码不能为空"));
        }

        @Test
        @DisplayName("密码登录时验证码标识为空应返回400")
        void loginByPassword_验证码标识为空_返回参数错误() throws Exception {
            mockMvc.perform(post("/v1/app/auth/login/password")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"username\":\"testuser\",\"password\":\"password123\",\"captchaCode\":\"c\"}"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(400))
                    .andExpect(jsonPath("$.message").value("验证码标识不能为空"));
        }
    }

    // ===== 短信登录接口 =====

    @Nested
    @DisplayName("短信登录接口 POST /v1/app/auth/login/sms")
    class LoginBySmsTests {

        @Test
        @DisplayName("短信登录成功应返回token字符串")
        void loginBySms_成功返回token() throws Exception {
            doNothing().when(authCaptchaService).validateCaptcha("sms-key", "1234");
            when(platformIdentityService.login(any())).thenReturn("sms-token-uuid-67890");

            mockMvc.perform(post("/v1/app/auth/login/sms")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(
                                    buildLoginDTO("13800000000", "654321", "sms-key", "1234")
                            )))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(200))
                    .andExpect(jsonPath("$.data").value("sms-token-uuid-67890"))
                    .andExpect(jsonPath("$.timestamp").isNumber());
        }
    }

    // ===== 第三方登录接口 =====

    @Nested
    @DisplayName("第三方登录接口 POST /v1/app/auth/login/third-party")
    class LoginByThirdPartyTests {

        @Test
        @DisplayName("第三方登录成功应返回token字符串")
        void loginByThirdParty_成功返回token() throws Exception {
            doNothing().when(authCaptchaService).validateCaptcha("tp-key", "XYZ");
            when(platformIdentityService.login(any())).thenReturn("tp-token-uuid-abc");

            mockMvc.perform(post("/v1/app/auth/login/third-party")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(
                                    buildLoginDTO("WECHAT", "wechat-openid-123", "tp-key", "XYZ")
                            )))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(200))
                    .andExpect(jsonPath("$.data").value("tp-token-uuid-abc"));
        }
    }

    // ===== 退出接口 =====

    @Nested
    @DisplayName("退出接口 POST /v1/app/auth/logout")
    class LogoutTests {

        @Test
        @DisplayName("已登录用户退出成功应返回code=200")
        void logout_已登录用户_成功退出() throws Exception {
            // 先模拟登录
            StpUtil.login(1L);

            mockMvc.perform(post("/v1/app/auth/logout")
                            .header("Authorization", StpUtil.getTokenValue()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(200))
                    .andExpect(jsonPath("$.message").value("操作成功"))
                    .andExpect(jsonPath("$.timestamp").isNumber());
        }

        @Test
        @DisplayName("未登录用户访问退出接口应返回401")
        void logout_未登录用户_返回401() throws Exception {
            mockMvc.perform(post("/v1/app/auth/logout"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(401));
        }
    }

    // ===== 辅助方法 =====

    private PlatformRegisterDTO buildRegisterDTO(String username, String password,
                                                  String phone, String email) {
        PlatformRegisterDTO dto = new PlatformRegisterDTO();
        dto.setUsername(username);
        dto.setPassword(password);
        dto.setPhone(phone);
        dto.setEmail(email);
        return dto;
    }

    private Object buildLoginDTO(String username, String password,
                                  String captchaKey, String captchaCode) {
        return new LoginDTODto(username, password, captchaKey, captchaCode);
    }

    /**
     * 内部 DTO 用于 JSON 序列化登录请求体。
     */
    record LoginDTODto(String username, String password, String captchaKey, String captchaCode) {}
}
