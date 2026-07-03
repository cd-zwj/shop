package com.payment.rag.service.scenario;

import com.payment.rag.service.AuthContextService;
import com.payment.service.AppOrderService;
import com.payment.service.CouponService;
import com.payment.service.MemberPointsAccountService;
import com.payment.service.MerchantWalletService;
import com.payment.service.RefundApplicationService;
import com.payment.service.SalesStatisticsService;
import com.payment.service.UnifiedWalletService;
import com.payment.service.V1AdminService;
import com.payment.service.WithdrawalService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@DisplayName("AI 场景业务工具权限测试")
class ScenarioBusinessToolsTest {

    private ScenarioBusinessTools createTools(AuthContextService authContextService) {
        return new ScenarioBusinessTools(
                authContextService,
                mock(UnifiedWalletService.class),
                mock(MemberPointsAccountService.class),
                mock(CouponService.class),
                mock(AppOrderService.class),
                mock(SalesStatisticsService.class),
                mock(RefundApplicationService.class),
                mock(MerchantWalletService.class),
                mock(WithdrawalService.class),
                mock(V1AdminService.class)
        );
    }

    @Test
    @DisplayName("角色正确但缺少权限时不能调用业务工具")
    void roleWithoutPermissionShouldRejectBusinessTool() {
        AuthContextService authContextService = mock(AuthContextService.class);
        when(authContextService.getCurrentRole()).thenReturn("merchant");
        when(authContextService.getCurrentPermissions()).thenReturn(List.of());

        ScenarioBusinessTools tools = createTools(authContextService);

        assertThrows(IllegalArgumentException.class, tools::merchantDataContext);
    }

    @Test
    @DisplayName("业务工具只返回当前权限允许的数据模块")
    void businessToolShouldOnlyExposePermittedModules() {
        AuthContextService authContextService = mock(AuthContextService.class);
        when(authContextService.getCurrentRole()).thenReturn("merchant");
        when(authContextService.getCurrentPermissions()).thenReturn(List.of("ai:merchant:orders"));
        when(authContextService.getCurrentPlatformUserId()).thenReturn(1001L);
        when(authContextService.getCurrentTenantId()).thenReturn(2001L);

        ScenarioBusinessTools tools = createTools(authContextService);
        String result = tools.merchantDataContext();

        assertTrue(result.contains("orders"));
        assertTrue(result.contains("refunds"));
        assertFalse(result.contains("finance"));
        assertFalse(result.contains("coupons"));
    }
}
