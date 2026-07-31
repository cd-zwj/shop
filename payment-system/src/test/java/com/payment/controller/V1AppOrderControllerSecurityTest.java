package com.payment.controller;

import com.payment.service.AppOrderService;
import com.payment.util.PlatformSessionHelper;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class V1AppOrderControllerSecurityTest {

    @Test
    void orderDetailShouldDisableClientAndProxyCaching() {
        AppOrderService service = mock(AppOrderService.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        V1AppOrderController controller = new V1AppOrderController(service);
        when(service.getOrderDetail(3L, "SO001")).thenReturn(null);

        try (MockedStatic<PlatformSessionHelper> session = mockStatic(PlatformSessionHelper.class)) {
            session.when(PlatformSessionHelper::getPlatformUserId).thenReturn(3L);
            controller.getOrder("SO001", response);
        }

        verify(response).setHeader("Cache-Control", "no-store");
    }
}
