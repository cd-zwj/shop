package com.payment.rag.service.scenario;

import com.payment.rag.Config.DateTimeTools;
import com.payment.rag.model.dto.AiScenario;
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
import com.payment.service.impl.MerchantStoreScopeService;
import com.payment.service.impl.V1MerchantSupportService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@DisplayName("AI 场景工具动态暴露测试")
class ScenarioToolExposureServiceTest {

    @Test
    @DisplayName("缺少权限时只暴露公共工具")
    void missingPermissionShouldOnlyExposeCommonTools() {
        AuthContextService authContextService = mock(AuthContextService.class);
        when(authContextService.getCurrentRole()).thenReturn("merchant");
        when(authContextService.getCurrentPermissions()).thenReturn(List.of());

        ScenarioToolExposureService service = buildService(authContextService);
        Object[] tools = service.exposedTools(AiScenario.MERCHANT_ORDER_ASSISTANT, mock(DateTimeTools.class));

        assertTrue(containsType(tools, DateTimeTools.class));
        assertTrue(containsType(tools, CommonScenarioTools.class));
        assertFalse(containsType(tools, MerchantScenarioTools.class));
    }

    @Test
    @DisplayName("具备场景权限时暴露对应角色工具")
    void permissionShouldExposeRoleSpecificTools() {
        AuthContextService authContextService = mock(AuthContextService.class);
        when(authContextService.getCurrentRole()).thenReturn("merchant");
        when(authContextService.getCurrentPermissions()).thenReturn(List.of("ai:merchant:orders"));

        ScenarioToolExposureService service = buildService(authContextService);
        Object[] tools = service.exposedTools(AiScenario.MERCHANT_ORDER_ASSISTANT, mock(DateTimeTools.class));

        assertTrue(containsType(tools, DateTimeTools.class));
        assertTrue(containsType(tools, CommonScenarioTools.class));
        assertTrue(containsType(tools, MerchantScenarioTools.class));
        assertFalse(containsType(tools, UserScenarioTools.class));
        assertFalse(containsType(tools, AdminScenarioTools.class));
    }

    private ScenarioToolExposureService buildService(AuthContextService authContextService) {
        ScenarioBusinessTools businessTools = new ScenarioBusinessTools(
                authContextService,
                mock(UnifiedWalletService.class),
                mock(MemberPointsAccountService.class),
                mock(CouponService.class),
                mock(AppOrderService.class),
                mock(SalesStatisticsService.class),
                mock(RefundApplicationService.class),
                mock(MerchantWalletService.class),
                mock(WithdrawalService.class),
                mock(V1AdminService.class),
                mock(MerchantStoreScopeService.class),
                mock(V1MerchantSupportService.class)
        );
        return new ScenarioToolExposureService(
                authContextService,
                new ScenarioToolRegistry(),
                new CommonScenarioTools(businessTools),
                new UserScenarioTools(businessTools),
                new MerchantScenarioTools(businessTools),
                new AdminScenarioTools(businessTools)
        );
    }

    private boolean containsType(Object[] tools, Class<?> type) {
        for (Object tool : tools) {
            if (type.isInstance(tool)) {
                return true;
            }
        }
        return false;
    }
}
