package com.payment.controller;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.payment.common.GlobalExceptionHandler;
import com.payment.config.TestSaTokenConfig;
import com.payment.config.TestRedissonConfig;
import com.payment.entity.Product;
import com.payment.entity.Tenant;
import com.payment.mapper.ProductMapper;
import com.payment.mapper.TenantMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import({TestSaTokenConfig.class, TestRedissonConfig.class, GlobalExceptionHandler.class})
@DisplayName("V1 商户与商品浏览集成测试")
class V1AppCatalogIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private TenantMapper tenantMapper;

    @MockBean
    private ProductMapper productMapper;

    private Tenant tenantA;
    private Tenant tenantB;
    private Product product1;
    private Product product2;
    private Product product3;

    @BeforeEach
    void setUp() {
        // 模拟登录态（浏览接口需要登录）
        try {
            StpUtil.login(1L);
        } catch (Exception ignored) {
        }

        tenantA = buildTenant(1L, "T001", "测试商户A", 1, 0);
        tenantB = buildTenant(2L, "T002", "测试商户B", 1, 0);

        product1 = buildProduct(1L, 1L, "P001", "经典咖啡",
                new BigDecimal("28.00"), "杯", "饮品", 1);
        product2 = buildProduct(2L, 1L, "P002", "抹茶拿铁",
                new BigDecimal("32.00"), "杯", "饮品", 1);
        product3 = buildProduct(3L, 2L, "P003", "手工蛋糕",
                new BigDecimal("45.00"), "个", "甜点", 1);
    }

    // ===== 商户列表 =====

    @Nested
    @DisplayName("商户列表 GET /v1/app/tenants")
    class ListTenantsTests {

        @Test
        @DisplayName("获取商户列表成功应返回数组，code=200")
        void listTenants_返回商户列表() throws Exception {
            when(tenantMapper.selectList(any(LambdaQueryWrapper.class)))
                    .thenReturn(List.of(tenantA, tenantB));

            mockMvc.perform(get("/v1/app/tenants")
                            .header("Authorization", StpUtil.getTokenValue()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(200))
                    .andExpect(jsonPath("$.message").value("操作成功"))
                    .andExpect(jsonPath("$.data", hasSize(2)))
                    .andExpect(jsonPath("$.data[0].id").value(1))
                    .andExpect(jsonPath("$.data[0].name").value("测试商户A"))
                    .andExpect(jsonPath("$.data[0].tenantCode").value("T001"))
                    .andExpect(jsonPath("$.data[1].id").value(2))
                    .andExpect(jsonPath("$.data[1].name").value("测试商户B"))
                    .andExpect(jsonPath("$.timestamp").isNumber());
        }

        @Test
        @DisplayName("无商户时应返回空数组")
        void listTenants_无商户_返回空数组() throws Exception {
            when(tenantMapper.selectList(any(LambdaQueryWrapper.class)))
                    .thenReturn(List.of());

            mockMvc.perform(get("/v1/app/tenants")
                            .header("Authorization", StpUtil.getTokenValue()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(200))
                    .andExpect(jsonPath("$.data", hasSize(0)));
        }

        @Test
        @DisplayName("未登录访问商户列表应返回401")
        void listTenants_未登录_返回401() throws Exception {
            try {
                StpUtil.logout();
            } catch (Exception ignored) {
            }

            mockMvc.perform(get("/v1/app/tenants"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(401));
        }
    }

    // ===== 商户详情 =====

    @Nested
    @DisplayName("商户详情 GET /v1/app/tenants/{tenantId}")
    class GetTenantTests {

        @Test
        @DisplayName("获取商户详情成功应返回商户对象")
        void getTenant_返回商户详情() throws Exception {
            when(tenantMapper.selectById(1L)).thenReturn(tenantA);

            mockMvc.perform(get("/v1/app/tenants/1")
                            .header("Authorization", StpUtil.getTokenValue()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(200))
                    .andExpect(jsonPath("$.data.id").value(1))
                    .andExpect(jsonPath("$.data.name").value("测试商户A"))
                    .andExpect(jsonPath("$.data.tenantCode").value("T001"))
                    .andExpect(jsonPath("$.data.contact").value("张三"))
                    .andExpect(jsonPath("$.data.phone").value("13900000000"))
                    .andExpect(jsonPath("$.timestamp").isNumber());
        }

        @Test
        @DisplayName("商户不存在时data应为null")
        void getTenant_商户不存在_返回null() throws Exception {
            when(tenantMapper.selectById(999L)).thenReturn(null);

            mockMvc.perform(get("/v1/app/tenants/999")
                            .header("Authorization", StpUtil.getTokenValue()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(200))
                    .andExpect(jsonPath("$.data").doesNotExist());
        }
    }

    // ===== 商品列表 =====

    @Nested
    @DisplayName("商品列表 GET /v1/app/tenants/{tenantId}/products")
    class ListProductsTests {

        @Test
        @DisplayName("获取商户商品列表成功应返回该商户的商品数组")
        void listProducts_返回商户商品() throws Exception {
            when(productMapper.selectList(any(LambdaQueryWrapper.class)))
                    .thenReturn(List.of(product1, product2));

            mockMvc.perform(get("/v1/app/tenants/1/products")
                            .header("Authorization", StpUtil.getTokenValue()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(200))
                    .andExpect(jsonPath("$.data", hasSize(2)))
                    .andExpect(jsonPath("$.data[0].name").value("经典咖啡"))
                    .andExpect(jsonPath("$.data[0].price").value(2800))
                    .andExpect(jsonPath("$.data[1].name").value("抹茶拿铁"))
                    .andExpect(jsonPath("$.timestamp").isNumber());
        }

        @Test
        @DisplayName("商户无商品时应返回空数组")
        void listProducts_无商品_返回空数组() throws Exception {
            when(productMapper.selectList(any(LambdaQueryWrapper.class)))
                    .thenReturn(List.of());

            mockMvc.perform(get("/v1/app/tenants/1/products")
                            .header("Authorization", StpUtil.getTokenValue()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(200))
                    .andExpect(jsonPath("$.data", hasSize(0)));
        }
    }

    // ===== 商品详情 =====

    @Nested
    @DisplayName("商品详情 GET /v1/app/products/{productId}")
    class GetProductTests {

        @Test
        @DisplayName("获取商品详情成功应返回完整商品信息")
        void getProduct_返回商品详情() throws Exception {
            when(productMapper.selectById(1L)).thenReturn(product1);

            mockMvc.perform(get("/v1/app/products/1")
                            .header("Authorization", StpUtil.getTokenValue()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(200))
                    .andExpect(jsonPath("$.data.id").value(1))
                    .andExpect(jsonPath("$.data.name").value("经典咖啡"))
                    .andExpect(jsonPath("$.data.price").value(2800))
                    .andExpect(jsonPath("$.data.unit").value("杯"))
                    .andExpect(jsonPath("$.data.category").value("饮品"))
                    .andExpect(jsonPath("$.data.status").value(1))
                    .andExpect(jsonPath("$.timestamp").isNumber());
        }

        @Test
        @DisplayName("商品不存在时data应为null")
        void getProduct_商品不存在_返回null() throws Exception {
            when(productMapper.selectById(999L)).thenReturn(null);

            mockMvc.perform(get("/v1/app/products/999")
                            .header("Authorization", StpUtil.getTokenValue()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(200))
                    .andExpect(jsonPath("$.data").doesNotExist());
        }
    }

    // ===== 辅助方法 =====

    private Tenant buildTenant(Long id, String code, String name, Integer status, Integer deleted) {
        Tenant t = new Tenant();
        t.setId(id);
        t.setTenantCode(code);
        t.setName(name);
        t.setContact("张三");
        t.setPhone("13900000000");
        t.setAddress("北京市朝阳区");
        t.setStatus(status);
        t.setDeleted(deleted);
        t.setCreateTime(LocalDateTime.of(2024, 1, 1, 0, 0, 0));
        t.setUpdateTime(LocalDateTime.of(2024, 1, 1, 0, 0, 0));
        return t;
    }

    private Product buildProduct(Long id, Long tenantId, String code, String name,
                                 BigDecimal price, String unit, String category, Integer status) {
        Product p = new Product();
        p.setId(id);
        p.setTenantId(tenantId);
        p.setProductCode(code);
        p.setName(name);
        p.setPrice(price);
        p.setUnit(unit);
        p.setCategory(category);
        p.setStatus(status);
        return p;
    }
}
