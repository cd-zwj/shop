package com.payment.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.payment.common.GlobalExceptionHandler;
import com.payment.config.TestSaTokenConfig;
import com.payment.config.TestRedissonConfig;
import com.payment.dto.AppUserVO;
import com.payment.dto.PlatformRegisterDTO;
import com.payment.dto.SmsLoginDTO;
import com.payment.entity.PlatformUser;
import com.payment.service.AuthCaptchaService;
import com.payment.service.LoginSecurityService;
import com.payment.service.PlatformEmailAccountService;
import com.payment.service.PlatformIdentityService;
import com.payment.service.SmsCodeService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.time.LocalDateTime;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import({TestSaTokenConfig.class, TestRedissonConfig.class, GlobalExceptionHandler.class})
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

    @MockBean
    private LoginSecurityService loginSecurityService;

    @MockBean
    private SmsCodeService smsCodeService;

    @MockBean
    private PlatformEmailAccountService platformEmailAccountService;

    private PlatformUser testUser;

    @BeforeEach
    void setUp() {
        // 每次测试前清除 Sa-Token 登录态
        SaTokenTestSupport.logoutPlatformUser();

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
        @DisplayName("注册时请求体为空应返回错误")
        void register_请求体为空_返回错误() throws Exception {
            mockMvc.perform(post("/v1/app/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(""))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(500));
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
        @DisplayName("短信登录成功应返回token字符串，code=200")
        void loginBySms_成功返回token() throws Exception {
            doNothing().when(authCaptchaService).validateCaptcha("sms-key", "1234");
            when(platformIdentityService.login(any())).thenReturn("sms-token-uuid-67890");

            MvcResult result = mockMvc.perform(post("/v1/app/auth/login/sms")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(
                                    buildSmsLoginDTO("13800000000", "654321", "sms-key", "1234")
                            )))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(200))
                    .andExpect(jsonPath("$.message").value("操作成功"))
                    .andExpect(jsonPath("$.data").isString())
                    .andExpect(jsonPath("$.data").value("sms-token-uuid-67890"))
                    .andExpect(jsonPath("$.timestamp").isNumber())
                    .andReturn();

            verify(authCaptchaService).validateCaptcha("sms-key", "1234");
            verify(platformIdentityService).login(any());
        }

        @Test
        @DisplayName("短信登录时手机号为空应返回400")
        void loginBySms_手机号为空_返回参数错误() throws Exception {
            mockMvc.perform(post("/v1/app/auth/login/sms")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"smsCode\":\"123456\",\"captchaKey\":\"k\",\"captchaCode\":\"c\"}"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(400))
                    .andExpect(jsonPath("$.message").value("手机号不能为空"));
        }

        @Test
        @DisplayName("短信登录时短信验证码为空应返回400")
        void loginBySms_短信验证码为空_返回参数错误() throws Exception {
            mockMvc.perform(post("/v1/app/auth/login/sms")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"phone\":\"13800000000\",\"captchaKey\":\"k\",\"captchaCode\":\"c\"}"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(400))
                    .andExpect(jsonPath("$.message").value("短信验证码不能为空"));
        }
    }

    // ===== 第三方登录接口 =====

    @Nested
    @DisplayName("第三方登录接口 POST /v1/app/auth/login/third-party")
    class LoginByThirdPartyTests {

        @Test
        @DisplayName("第三方登录成功应返回token字符串，code=200")
        void loginByThirdParty_成功返回token() throws Exception {
            doNothing().when(authCaptchaService).validateCaptcha("wx-key", "WX12");
            when(platformIdentityService.login(any())).thenReturn("wx-token-uuid-abcde");

            MvcResult result = mockMvc.perform(post("/v1/app/auth/login/third-party")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(
                                    buildLoginDTO("wx_openid_123", "wx_unionid_456", "wx-key", "WX12")
                            )))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(200))
                    .andExpect(jsonPath("$.message").value("操作成功"))
                    .andExpect(jsonPath("$.data").isString())
                    .andExpect(jsonPath("$.data").value("wx-token-uuid-abcde"))
                    .andExpect(jsonPath("$.timestamp").isNumber())
                    .andReturn();

            verify(authCaptchaService).validateCaptcha("wx-key", "WX12");
            verify(platformIdentityService).login(any());
        }

        @Test
        @DisplayName("第三方登录时用户名为空应返回400")
        void loginByThirdParty_用户名为空_返回参数错误() throws Exception {
            mockMvc.perform(post("/v1/app/auth/login/third-party")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"password\":\"wx_unionid_456\",\"captchaKey\":\"k\",\"captchaCode\":\"c\"}"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(400))
                    .andExpect(jsonPath("$.message").value("用户名不能为空"));
        }
    }

    // ===== 密码重置接口 =====

    @Nested
    @DisplayName("密码重置接口")
    class PasswordResetTests {

        @Test
        @DisplayName("发送密码重置邮箱验证码应校验图形验证码并调用邮箱服务")
        void sendPasswordResetCode_成功发送() throws Exception {
            doNothing().when(authCaptchaService).validateCaptcha("captcha-key", "ABCD");

            mockMvc.perform(post("/v1/app/auth/password/reset/send-code")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                      "email": "test@example.com",
                                      "captchaKey": "captcha-key",
                                      "captchaCode": "ABCD"
                                    }
                                    """))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(200))
                    .andExpect(jsonPath("$.message").value("操作成功"))
                    .andExpect(jsonPath("$.timestamp").isNumber());

            verify(authCaptchaService).validateCaptcha("captcha-key", "ABCD");
            verify(platformEmailAccountService).sendRecoverCode("test@example.com");
        }

        @Test
        @DisplayName("提交密码重置应调用邮箱账号服务重置密码")
        void resetPassword_成功重置() throws Exception {
            mockMvc.perform(post("/v1/app/auth/password/reset/verify")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                      "email": "test@example.com",
                                      "emailCode": "123456",
                                      "newPassword": "newPass123"
                                    }
                                    """))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(200))
                    .andExpect(jsonPath("$.message").value("操作成功"))
                    .andExpect(jsonPath("$.timestamp").isNumber());

            verify(platformEmailAccountService).resetPassword("test@example.com", "123456", "newPass123");
        }

        @Test
        @DisplayName("发送密码重置验证码时邮箱格式错误应返回400")
        void sendPasswordResetCode_邮箱格式错误_返回参数错误() throws Exception {
            mockMvc.perform(post("/v1/app/auth/password/reset/send-code")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                      "email": "bad-email",
                                      "captchaKey": "captcha-key",
                                      "captchaCode": "ABCD"
                                    }
                                    """))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(400))
                    .andExpect(jsonPath("$.message").value("邮箱格式不正确"));

            verifyNoInteractions(platformEmailAccountService);
        }

        @Test
        @DisplayName("提交密码重置时新密码过短应返回400")
        void resetPassword_新密码过短_返回参数错误() throws Exception {
            mockMvc.perform(post("/v1/app/auth/password/reset/verify")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                      "email": "test@example.com",
                                      "emailCode": "123456",
                                      "newPassword": "12345"
                                    }
                                    """))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(400))
                    .andExpect(jsonPath("$.message").value("密码长度需在6-64位之间"));

            verifyNoInteractions(platformEmailAccountService);
        }
    }

    // ===== 退出登录接口 =====

    @Nested
    @DisplayName("退出登录接口 POST /v1/app/auth/logout")
    class LogoutTests {

        @Test
        @DisplayName("退出登录应返回200")
        void logout_成功退出() throws Exception {
            // 先登录
            String token = SaTokenTestSupport.loginPlatformUser(1L);

            mockMvc.perform(post("/v1/app/auth/logout")
                            .header("Authorization", token))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(200))
                    .andExpect(jsonPath("$.message").value("操作成功"))
                    .andExpect(jsonPath("$.timestamp").isNumber());
        }

        @Test
        @DisplayName("未登录退出应返回401")
        void logout_未登录_返回401() throws Exception {
            mockMvc.perform(post("/v1/app/auth/logout"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(401))
                    .andExpect(jsonPath("$.message").value("未提供Token"));
        }
    }

    // ===== 辅助方法 =====

    private PlatformRegisterDTO buildRegisterDTO(String username, String password, String phone, String email) {
        PlatformRegisterDTO dto = new PlatformRegisterDTO();
        dto.setUsername(username);
        dto.setPassword(password);
        dto.setPhone(phone);
        dto.setEmail(email);
        return dto;
    }

    private com.payment.dto.PlatformLoginDTO buildLoginDTO(String username, String password, String captchaKey, String captchaCode) {
        com.payment.dto.PlatformLoginDTO dto = new com.payment.dto.PlatformLoginDTO();
        dto.setUsername(username);
        dto.setPassword(password);
        dto.setCaptchaKey(captchaKey);
        dto.setCaptchaCode(captchaCode);
        return dto;
    }

    private SmsLoginDTO buildSmsLoginDTO(String phone, String smsCode, String captchaKey, String captchaCode) {
        SmsLoginDTO dto = new SmsLoginDTO();
        dto.setPhone(phone);
        dto.setSmsCode(smsCode);
        dto.setCaptchaKey(captchaKey);
        dto.setCaptchaCode(captchaCode);
        return dto;
    }
}
