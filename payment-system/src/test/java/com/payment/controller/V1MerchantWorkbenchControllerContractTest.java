package com.payment.controller;

import com.payment.common.Result;
import com.payment.dto.MerchantWorkbenchTodoSummaryVO;
import org.junit.jupiter.api.Test;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class V1MerchantWorkbenchControllerContractTest {

    @Test
    void controllerShouldExposeMerchantWorkbenchTodoContract() throws NoSuchMethodException {
        RequestMapping mapping = V1MerchantWorkbenchController.class.getAnnotation(RequestMapping.class);
        Method method = V1MerchantWorkbenchController.class.getMethod("getTodoSummary", Long.class);

        assertNotNull(mapping);
        assertEquals("/v1/merchant/tenants/{tenantId}/workbench", mapping.value()[0]);
        assertEquals("/todos", method.getAnnotation(GetMapping.class).value()[0]);

        ParameterizedType resultType = (ParameterizedType) method.getGenericReturnType();
        assertEquals(Result.class, resultType.getRawType());
        assertEquals(MerchantWorkbenchTodoSummaryVO.class, resultType.getActualTypeArguments()[0]);
    }
}
