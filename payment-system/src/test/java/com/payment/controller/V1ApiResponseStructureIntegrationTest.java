package com.payment.controller;

import cn.dev33.satoken.stp.StpUtil;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.payment.common.GlobalExceptionHandler;
import com.payment.config.TestSaTokenConfig;
import com.payment.dto.AppUserVO;
import com.payment.dto.PlatformRegisterDTO;
import com.payment.entity.PlatformUser;
import com.payment.entity.Tenant;
import com.payment.entity.Product;
import com.payment.mapper.ProductMapper;
import com.payment.mapper.TenantMapper;
import com.payment.service.AuthCaptchaService;
import com.payment.service.PlatformIdentityService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * V1 接口返回结构与异常场景集成测试。
 * <p>
 * 验证：
 * 1. 所有接口统一返回 { code, message, data, timestamp }
 * 2. 异常场景正确处理（未登录、无效token、参数校验失败）
 */
@WebMvcTest({V1AppAuthController.class, V1AppCatalogController.class, V1AppUserController.class})
@Import({TestSaTokenConfig.class, GlobalExceptionHandler.class})
@DisplayName("V1 接口返回结构与异常场景集成测试")
class V1ApiResponseStructureIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private AuthCaptchaService authCaptchaService;

    @MockBean
    private PlatformIdentityService platformIdentityService;

    @MockBean
    private TenantMapper tenantMapper;

    @MockBean
    private ProductMapper productMapper;

    @BeforeEach
    void setUp() {
        try {
            StpUtil.logout();
        } catch (Exception ignored) {
        }
    }

    // ===== 统一返回结构验证 =====

    @Nested
    @DisplayName("统一返回结构 { code, message, data, timestamp }")
    class ResponseStructureTests {

        @Test
        @DisplayName("注册接口返回结构包含 code/message/timestamp")
        void register_返回结构正确() throws Exception {
            PlatformUser user = new PlatformUser();
            user.setId(1L);
            user.setUserNo("U001");
            user.setUsername("newuser");
            user.setStatus(1);
            user.setCreateTime(LocalDateTime.now());
            when(platformIdentityService.register(any(PlatformRegisterDTO.class))).thenReturn(user);

            MvcResult result = mockMvc.perform(post("/v1/app/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"username\":\"newuser\",\"password\":\"password123\"}"))
                    .andReturn();

            JsonNode root = objectMapper.readTree(result.getResponse().getContentAsString());
            assertResponseStructure(root, "注册接口");
        }

        @Test
        @DisplayName("密码登录接口返回结构包含 code/message/data/timestamp")
        void loginByPassword_返回结构正确() throws Exception {
            doNothing().when(authCaptchaService).validateCaptcha(any(), any());
            when(platformIdentityService.login(any())).thenReturn("token-123");

            MvcResult result = mockMvc.perform(post("/v1/app/auth/login/password")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"username\":\"u\",\"password\":\"p\",\"captchaKey\":\"k\",\"captchaCode\":\"c\"}"))
                    .andReturn();

            JsonNode root = objectMapper.readTree(result.getResponse().getContentAsString());
            assertResponseStructure(root, "登录接口");
            assertTrue(root.has("data"), "登录接口应包含data字段(token)");
        }

        @Test
        @DisplayName("商户列表接口返回结构包含 code/message/data/timestamp")
        void listTenants_返回结构正确() throws Exception {
            StpUtil.login(1L);
            when(tenantMapper.selectList(any())).thenReturn(List.of());

            MvcResult result = mockMvc.perform(get("/v1/app/tenants")
                            .header("Authorization", StpUtil.getTokenValue()))
                    .andReturn();

            JsonNode root = objectMapper.readTree(result.getResponse().getContentAsString());
            assertResponseStructure(root, "商户列表接口");
        }

        @Test
        @DisplayName("商户详情接口返回结构包含 code/message/data/timestamp")
        void getTenant_返回结构正确() throws Exception {
            StpUtil.login(1L);
            Tenant t = new Tenant();
            t.setId(1L);
            t.setTenantCode("T001");
            t.setName("测试商户");
            t.setStatus(1);
            when(tenantMapper.selectById(1L)).thenReturn(t);

            MvcResult result = mockMvc.perform(get("/v1/app/tenants/1")
                            .header("Authorization", StpUtil.getTokenValue()))
                    .andReturn();

            JsonNode root = objectMapper.readTree(result.getResponse().getContentAsString());
            assertResponseStructure(root, "商户详情接口");
        }

        @Test
        @DisplayName("商品列表接口返回结构包含 code/message/data/timestamp")
        void listProducts_返回结构正确() throws Exception {
            StpUtil.login(1L);
            when(productMapper.selectList(any())).thenReturn(List.of());

            MvcResult result = mockMvc.perform(get("/v1/app/tenants/1/products")
                            .header("Authorization", StpUtil.getTokenValue()))
                    .andReturn();

            JsonNode root = objectMapper.readTree(result.getResponse().getContentAsString());
            assertResponseStructure(root, "商品列表接口");
        }

        @Test
        @DisplayName("商品详情接口返回结构包含 code/message/data/timestamp")
        void getProduct_返回结构正确() throws Exception {
            StpUtil.login(1L);
            Product p = new Product();
            p.setId(1L);
            p.setName("商品A");
            p.setPrice(new BigDecimal("10.00"));
            p.setTenantId(1L);
            when(productMapper.selectById(1L)).thenReturn(p);

            MvcResult result = mockMvc.perform(get("/v1/app/products/1")
                            .header("Authorization", StpUtil.getTokenValue()))
                    .andReturn();

            JsonNode root = objectMapper.readTree(result.getResponse().getContentAsString());
            assertResponseStructure(root, "商品详情接口");
        }

        @Test
        @DisplayName("用户信息接口返回结构包含 code/message/data/timestamp")
        void getCurrentUser_返回结构正确() throws Exception {
            StpUtil.login(1L);
            PlatformUser user = new PlatformUser();
            user.setId(1L);
            user.setUserNo("U001");
            user.setUsername("testuser");
            user.setStatus(1);
            user.setCreateTime(LocalDateTime.now());
            when(platformIdentityService.getCurrentUser()).thenReturn(user);

            MvcResult result = mockMvc.perform(get("/v1/app/users/me")
                            .header("Authorization", StpUtil.getTokenValue()))
                    .andReturn();

            JsonNode root = objectMapper.readTree(result.getResponse().getContentAsString());
            assertResponseStructure(root, "用户信息接口");
        }

        @Test
        @DisplayName("未登录接口返回结构也包含 code/message/timestamp")
        void notLogin_返回结构正确() throws Exception {
            MvcResult result = mockMvc.perform(get("/v1/app/users/me"))
                    .andReturn();

            JsonNode root = objectMapper.readTree(result.getResponse().getContentAsString());
            assertResponseStructure(root, "未登录访问");
            assertEquals(401, root.get("code").asInt());
        }

        @Test
        @DisplayName("参数校验失败返回结构也包含 code/message/timestamp")
        void paramError_返回结构正确() throws Exception {
            MvcResult result = mockMvc.perform(post("/v1/app/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"password\":\"password123\"}"))
                    .andReturn();

            JsonNode root = objectMapper.readTree(result.getResponse().getContentAsString());
            assertResponseStructure(root, "参数校验失败");
            assertEquals(400, root.get("code").asInt());
        }
    }

    // ===== 异常场景验证 =====

    @Nested
    @DisplayName("异常场景")
    class ExceptionScenarioTests {

        @Test
        @DisplayName("未登录访问需认证接口应返回401 + '未提供Token'")
        void notAuthenticated_返回未提供Token() throws Exception {
            mockMvc.perform(get("/v1/app/users/me"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(401))
                    .andExpect(jsonPath("$.message").value("未提供Token"))
                    .andExpect(jsonPath("$.timestamp").isNumber());
        }

        @Test
        @DisplayName("无效Token应返回401 + 'Token无效'")
        void invalidToken_返回Token无效() throws Exception {
            mockMvc.perform(get("/v1/app/users/me")
                            .header("Authorization", "bad-token-123"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(401))
                    .andExpect(jsonPath("$.message").value("Token无效"));
        }

        @Test
        @DisplayName("注册时用户名为空应返回400")
        void register_缺少用户名_返回400() throws Exception {
            mockMvc.perform(post("/v1/app/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"password\":\"password123\"}"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(400))
                    .andExpect(jsonPath("$.message").value("用户名不能为空"));
        }

        @Test
        @DisplayName("登录时密码为空应返回400")
        void login_缺少密码_返回400() throws Exception {
            mockMvc.perform(post("/v1/app/auth/login/password")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"username\":\"testuser\",\"captchaKey\":\"k\",\"captchaCode\":\"c\"}"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(400))
                    .andExpect(jsonPath("$.message").value("密码不能为空"));
        }

        @Test
        @DisplayName("登录时验证码标识为空应返回400")
        void login_缺少验证码标识_返回400() throws Exception {
            mockMvc.perform(post("/v1/app/auth/login/password")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"username\":\"testuser\",\"password\":\"password123\",\"captchaCode\":\"c\"}"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(400))
                    .andExpect(jsonPath("$.message").value("验证码标识不能为空"));
        }

        @Test
        @DisplayName("登录时图形验证码为空应返回400")
        void login_缺少验证码_返回400() throws Exception {
            mockMvc.perform(post("/v1/app/auth/login/password")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"username\":\"testuser\",\"password\":\"password123\",\"captchaKey\":\"k\"}"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(400))
                    .andExpect(jsonPath("$.message").value("图形验证码不能为空"));
        }

        @Test
        @DisplayName("注册时密码长度不足6位应返回400")
        void register_密码太短_返回400() throws Exception {
            mockMvc.perform(post("/v1/app/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"username\":\"newuser\",\"password\":\"12345\"}"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(400))
                    .andExpect(jsonPath("$.message").value("密码长度需在6-64位之间"));
        }

        @Test
        @DisplayName("注册时空JSON体应返回400")
        void register_空JSON_返回400() throws Exception {
            mockMvc.perform(post("/v1/app/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{}"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(400));
        }
    }

    // ===== 辅助断言方法 =====

    /**
     * 断言响应结构包含必要字段。
     */
    private void assertResponseStructure(JsonNode root, String apiName) {
        assertTrue(root.has("code"), apiName + " 响应应包含 'code' 字段");
        assertTrue(root.has("message"), apiName + " 响应应包含 'message' 字段");
        assertTrue(root.has("timestamp"), apiName + " 响应应包含 'timestamp' 字段");

        assertTrue(root.get("code").isInt(), apiName + " 'code' 应为整数");
        assertTrue(root.get("message").isTextual(), apiName + " 'message' 应为字符串");
        assertTrue(root.get("timestamp").isNumber(), apiName + " 'timestamp' 应为数字");

        // timestamp 应为合理的时间戳（大于 2020-01-01 的毫秒数）
        long timestamp = root.get("timestamp").asLong();
        assertTrue(timestamp > 1577836800000L,
                apiName + " timestamp 应为合理的时间戳，实际值: " + timestamp);
    }
}
