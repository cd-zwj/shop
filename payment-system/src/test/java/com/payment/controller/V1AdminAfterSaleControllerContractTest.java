package com.payment.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.payment.common.PageResult;
import com.payment.common.Result;
import com.payment.vo.AdminAfterSaleVO;
import com.payment.vo.AfterSaleActionVO;
import org.junit.jupiter.api.Test;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;

import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class V1AdminAfterSaleControllerContractTest {

    @Test
    void adminAfterSaleEndpointsShouldUseDedicatedReadAndManagePermissions() throws Exception {
        Method list = V1AdminAfterSaleController.class.getMethod(
                "listRefunds", Long.class, String.class, String.class, Integer.class, Integer.class);
        Method detail = V1AdminAfterSaleController.class.getMethod("getRefund", Long.class, Long.class);
        Method actions = V1AdminAfterSaleController.class.getMethod("listActions", Long.class, Long.class);
        Method intervene = V1AdminAfterSaleController.class.getMethod(
                "intervene", Long.class, Long.class, V1AdminAfterSaleController.InterventionRequest.class);

        assertEquals("/refunds", list.getAnnotation(GetMapping.class).value()[0]);
        assertPageResultData(list, AdminAfterSaleVO.class);
        assertPermission(list, "admin:after-sale:list");

        assertEquals("/tenants/{tenantId}/refunds/{refundId}", detail.getAnnotation(GetMapping.class).value()[0]);
        assertResultData(detail, AdminAfterSaleVO.class);
        assertPermission(detail, "admin:after-sale:list");

        assertEquals("/tenants/{tenantId}/refunds/{refundId}/actions", actions.getAnnotation(GetMapping.class).value()[0]);
        assertListResult(actions, AfterSaleActionVO.class);
        assertPermission(actions, "admin:after-sale:list");

        assertEquals("/tenants/{tenantId}/refunds/{refundId}/intervene", intervene.getAnnotation(PutMapping.class).value()[0]);
        assertPermission(intervene, "admin:after-sale:manage");
    }

    @Test
    void interventionDecisionShouldNotDefaultMissingApprovedToRejection() {
        Class<?> approvedType = java.util.Arrays.stream(
                        V1AdminAfterSaleController.InterventionRequest.class.getDeclaredFields())
                .filter(field -> "approved".equals(field.getName()))
                .findFirst()
                .orElseThrow()
                .getType();

        assertEquals(Boolean.class, approvedType);
    }

    private void assertPermission(Method method, String permission) {
        SaCheckPermission annotation = method.getAnnotation(SaCheckPermission.class);
        assertNotNull(annotation);
        assertEquals(permission, annotation.value()[0]);
    }

    private void assertPageResultData(Method method, Class<?> itemType) {
        ParameterizedType resultType = (ParameterizedType) method.getGenericReturnType();
        assertEquals(Result.class, resultType.getRawType());
        ParameterizedType pageType = (ParameterizedType) resultType.getActualTypeArguments()[0];
        assertEquals(PageResult.class, pageType.getRawType());
        assertEquals(itemType, pageType.getActualTypeArguments()[0]);
    }

    private void assertResultData(Method method, Class<?> dataType) {
        ParameterizedType resultType = (ParameterizedType) method.getGenericReturnType();
        assertEquals(Result.class, resultType.getRawType());
        assertEquals(dataType, resultType.getActualTypeArguments()[0]);
    }

    private void assertListResult(Method method, Class<?> itemType) {
        ParameterizedType resultType = (ParameterizedType) method.getGenericReturnType();
        ParameterizedType listType = (ParameterizedType) resultType.getActualTypeArguments()[0];
        assertEquals(List.class, listType.getRawType());
        assertEquals(itemType, listType.getActualTypeArguments()[0]);
    }
}
