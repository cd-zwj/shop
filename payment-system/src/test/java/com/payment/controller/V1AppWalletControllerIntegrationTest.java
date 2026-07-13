package com.payment.controller;

import com.payment.common.GlobalExceptionHandler;
import com.payment.config.TestRedissonConfig;
import com.payment.config.TestSaTokenConfig;
import com.payment.dto.AppAssetActivityVO;
import com.payment.dto.AssetActivityPageVO;
import com.payment.dto.AssetActivityQueryDTO;
import com.payment.dto.AppTenantAssetSummaryVO;
import com.payment.dto.AssetHoldVO;
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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
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
    void listAssetActivitiesShouldKeepLegacyArrayResponseAndSizeParameter() throws Exception {
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
        when(appAssetSummaryService.listAssetActivities(99L, 10)).thenReturn(List.of(activity));

        mockMvc.perform(get("/v1/app/assets/activities")
                        .param("size", "10")
                        .header("Authorization", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data[0].assetType").value("COUPON"))
                .andExpect(jsonPath("$.data[0].title").value("优惠券核销"))
                .andExpect(jsonPath("$.data[0].tenantName").value("本地测试店"))
                .andExpect(jsonPath("$.data[0].actionPath").value("/coupons?tenantId=9"));

        verify(appAssetSummaryService).listAssetActivities(99L, 10);
    }

    @Test
    void listAssetActivitiesPageShouldReturnCursorPage() throws Exception {
        AppAssetActivityVO activity = new AppAssetActivityVO();
        activity.setAssetType("POINTS");
        activity.setTenantId(9L);
        when(appAssetSummaryService.listAssetActivities(eq(99L), any(AssetActivityQueryDTO.class)))
                .thenReturn(new AssetActivityPageVO(List.of(activity), "next-cursor", true));

        mockMvc.perform(get("/v1/app/assets/activities/page")
                        .param("size", "10")
                        .param("types", "POINTS")
                        .param("tenantId", "9")
                        .header("Authorization", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.records[0].assetType").value("POINTS"))
                .andExpect(jsonPath("$.data.nextCursor").value("next-cursor"))
                .andExpect(jsonPath("$.data.hasMore").value(true));
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

    @Test
    void listAssetHoldsShouldAllowAllTenantsAndExposeTenantName() throws Exception {
        AssetHoldVO hold = new AssetHoldVO();
        hold.setTenantId(9L);
        hold.setTenantName("本地测试店");
        hold.setAssetType("POINTS");
        hold.setHoldStatus("PRE_HOLD");
        hold.setAmountText("-20 积分");
        hold.setReason("订单待支付");
        hold.setBizType("SALES_ORDER");
        hold.setBizNo("SO1001");
        hold.setOccurredAt(LocalDateTime.of(2026, 7, 11, 10, 0));
        hold.setActionPath("/order/SO1001");
        when(appAssetSummaryService.listAssetHolds(99L, null)).thenReturn(List.of(hold));

        mockMvc.perform(get("/v1/app/assets/holds")
                        .header("Authorization", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data[0].tenantName").value("本地测试店"))
                .andExpect(jsonPath("$.data[0].assetType").value("POINTS"))
                .andExpect(jsonPath("$.data[0].holdStatus").value("PRE_HOLD"))
                .andExpect(jsonPath("$.data[0].bizNo").value("SO1001"))
                .andExpect(jsonPath("$.data[0].actionPath").value("/order/SO1001"));

        verify(appAssetSummaryService).listAssetHolds(99L, null);
    }
}
