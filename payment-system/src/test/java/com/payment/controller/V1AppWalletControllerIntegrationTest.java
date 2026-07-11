package com.payment.controller;

import com.payment.common.GlobalExceptionHandler;
import com.payment.config.TestRedissonConfig;
import com.payment.config.TestSaTokenConfig;
import com.payment.dto.AppAssetActivityVO;
import com.payment.dto.AppTenantAssetSummaryVO;
import com.payment.service.AppAssetSummaryService;
import org.junit.jupiter.api.BeforeEach;
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

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import({TestSaTokenConfig.class, TestRedissonConfig.class, GlobalExceptionHandler.class})
class V1AppWalletControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AppAssetSummaryService appAssetSummaryService;

    private String token;

    @BeforeEach
    void setUp() {
        token = SaTokenTestSupport.loginPlatformUser(99L);
    }

    @Test
    void listAssetActivitiesShouldReturnAuthenticatedUsersActivities() throws Exception {
        AppAssetActivityVO activity = new AppAssetActivityVO();
        activity.setAssetType("COUPON");
        activity.setTitle("优惠券核销");
        activity.setDescription("订单 SO1001 已使用优惠券");
        activity.setOccurredAt(LocalDateTime.of(2026, 7, 11, 10, 30));
        activity.setTenantId(9L);
        activity.setTenantName("本地测试店");
        activity.setBizNo("SO1001");
        activity.setAmountText("¥8");
        activity.setTone("positive");
        activity.setActionPath("/coupons?tenantId=9");
        when(appAssetSummaryService.listAssetActivities(eq(99L), eq(10))).thenReturn(List.of(activity));

        mockMvc.perform(get("/v1/app/assets/activities")
                        .param("size", "10")
                        .header("Authorization", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data[0].assetType").value("COUPON"))
                .andExpect(jsonPath("$.data[0].title").value("优惠券核销"))
                .andExpect(jsonPath("$.data[0].tenantName").value("本地测试店"))
                .andExpect(jsonPath("$.data[0].actionPath").value("/coupons?tenantId=9"));
    }

    @Test
    void listTenantAssetSummariesShouldExposeExpiringCouponCount() throws Exception {
        AppTenantAssetSummaryVO summary = new AppTenantAssetSummaryVO();
        summary.setTenantId(9L);
        summary.setTenantName("本地测试店");
        summary.setWalletAvailableAmount(new BigDecimal("18.00"));
        summary.setPoints(120);
        summary.setUsableCouponCount(3);
        summary.setExpiringSoonCouponCount(2);
        when(appAssetSummaryService.listTenantAssetSummaries(99L)).thenReturn(List.of(summary));

        mockMvc.perform(get("/v1/app/assets/tenant-summaries")
                        .header("Authorization", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data[0].tenantId").value(9))
                .andExpect(jsonPath("$.data[0].usableCouponCount").value(3))
                .andExpect(jsonPath("$.data[0].expiringSoonCouponCount").value(2));
    }
}
