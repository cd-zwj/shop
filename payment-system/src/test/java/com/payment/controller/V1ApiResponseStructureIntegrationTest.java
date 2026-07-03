package com.payment.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.payment.common.GlobalExceptionHandler;
import com.payment.config.TestSaTokenConfig;
import com.payment.config.TestRedissonConfig;
import com.payment.entity.PlatformUser;
import com.payment.entity.Tenant;
import com.payment.entity.Product;
import com.payment.mapper.ProductMapper;
import com.payment.mapper.TenantMapper;
import com.payment.service.AuthCaptchaService;
import com.payment.service.LoginSecurityService;
import com.payment.service.PlatformIdentityService;
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

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import({TestSaTokenConfig.class, TestRedissonConfig.class, GlobalExceptionHandler.class})
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
    private LoginSecurityService loginSecurityService;

    @MockBean
    private TenantMapper tenantMapper;

    @MockBean
    private ProductMapper productMapper;

    @BeforeEach
    void setUp() {
        SaTokenTestSupport.logoutPlatformUser();
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
            when(platformIdentityService.register(any())).thenReturn(user);

            MvcResult result = mockMvc.perform(post("/v1/app/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"username\":\"newuser\",\"password\":\"password123\"}"))
                    .andReturn();

            com.fasterxml.jackson.databind.JsonNode root = objectMapper.readTree(result.getResponse().getContentAsString());
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

            com.fasterxml.jackson.databind.JsonNode root = objectMapper.readTree(result.getResponse().getContentAsString());
            assertResponseStructure(root, "登录接口");
            assertTrue(root.has("data"), "登录接口应包含data字段(token)");
        }

        @Test
        @DisplayName("商户列表接口返回结构包含 code/message/data/timestamp")
        void listTenants_返回结构正确() throws Exception {
            String token = SaTokenTestSupport.loginPlatformUser(1L);
            when(tenantMapper.selectList(any())).thenReturn(List.of());

            MvcResult result = mockMvc.perform(get("/v1/app/tenants")
                            .header("Authorization", token))
                    .andReturn();

            com.fasterxml.jackson.databind.JsonNode root = objectMapper.readTree(result.getResponse().getContentAsString());
            assertResponseStructure(root, "商户列表接口");
        }

        @Test
        @DisplayName("商户详情接口返回结构包含 code/message/data/timestamp")
        void getTenant_返回结构正确() throws Exception {
            String token = SaTokenTestSupport.loginPlatformUser(1L);
            Tenant t = new Tenant();
            t.setId(1L);
            t.setTenantCode("T001");
            t.setName("测试商户");
            t.setStatus(1);
            when(tenantMapper.selectById(1L)).thenReturn(t);

            MvcResult result = mockMvc.perform(get("/v1/app/tenants/1")
                            .header("Authorization", token))
                    .andReturn();

            com.fasterxml.jackson.databind.JsonNode root = objectMapper.readTree(result.getResponse().getContentAsString());
            assertResponseStructure(root, "商户详情接口");
        }

        @Test
        @DisplayName("商品列表接口返回结构包含 code/message/data/timestamp")
        void listProducts_返回结构正确() throws Exception {
            String token = SaTokenTestSupport.loginPlatformUser(1L);
            when(productMapper.selectList(any())).thenReturn(List.of());

            MvcResult result = mockMvc.perform(get("/v1/app/tenants/1/products")
                            .header("Authorization", token))
                    .andReturn();

            com.fasterxml.jackson.databind.JsonNode root = objectMapper.readTree(result.getResponse().getContentAsString());
            assertResponseStructure(root, "商品列表接口");
        }

        @Test
        @DisplayName("商品详情接口返回结构包含 code/message/data/timestamp")
        void getProduct_返回结构正确() throws Exception {
            String token = SaTokenTestSupport.loginPlatformUser(1L);
            Product p = new Product();
            p.setId(1L);
            p.setName("商品A");
            p.setPrice(new BigDecimal("10.00"));
            p.setTenantId(1L);
            when(productMapper.selectById(1L)).thenReturn(p);

            MvcResult result = mockMvc.perform(get("/v1/app/products/1")
                            .header("Authorization", token))
                    .andReturn();

            com.fasterxml.jackson.databind.JsonNode root = objectMapper.readTree(result.getResponse().getContentAsString());
            assertResponseStructure(root, "商品详情接口");
        }
    }

    // ===== 异常场景验证 =====

    @Nested
    @DisplayName("异常场景验证")
    class ExceptionScenarioTests {

        @Test
        @DisplayName("未登录访问需要登录的接口应返回401")
        void unauthenticatedAccess_返回401() throws Exception {
            mockMvc.perform(get("/v1/app/users/me"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(401));
        }

        @Test
        @DisplayName("使用无效Token访问应返回401")
        void invalidToken_返回401() throws Exception {
            mockMvc.perform(get("/v1/app/users/me")
                            .header("Authorization", "invalid-token"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(401));
        }
    }

    private void assertResponseStructure(com.fasterxml.jackson.databind.JsonNode root, String apiName) {
        assertTrue(root.has("code"), apiName + " 应包含 code 字段");
        assertTrue(root.has("message"), apiName + " 应包含 message 字段");
        assertTrue(root.has("timestamp"), apiName + " 应包含 timestamp 字段");
    }
}
