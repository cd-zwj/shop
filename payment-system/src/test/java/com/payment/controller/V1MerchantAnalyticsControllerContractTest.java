package com.payment.controller;

import com.payment.common.Result;
import com.payment.dto.ProductSalesRankDTO;
import org.junit.jupiter.api.Test;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class V1MerchantAnalyticsControllerContractTest {

    @Test
    void controllerShouldExposeProductRankContract() throws NoSuchMethodException {
        RequestMapping mapping = V1MerchantAnalyticsController.class.getAnnotation(RequestMapping.class);
        Method method = V1MerchantAnalyticsController.class.getMethod(
                "getProductSalesRank",
                Long.class,
                LocalDate.class,
                LocalDate.class,
                Integer.class
        );

        assertNotNull(mapping);
        assertEquals("/v1/merchant/tenants/{tenantId}/analytics", mapping.value()[0]);
        assertEquals("/product-rank", method.getAnnotation(GetMapping.class).value()[0]);

        ParameterizedType resultType = (ParameterizedType) method.getGenericReturnType();
        assertEquals(Result.class, resultType.getRawType());
        Type dataType = resultType.getActualTypeArguments()[0];
        assertEquals(List.class, ((ParameterizedType) dataType).getRawType());
        assertEquals(ProductSalesRankDTO.class, ((ParameterizedType) dataType).getActualTypeArguments()[0]);
    }
}
