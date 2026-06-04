package com.payment.controller;

import com.payment.annotation.RateLimit;
import com.payment.common.Result;
import com.payment.dto.AppCouponReceiveVO;
import com.payment.dto.AppCouponTemplateVO;
import com.payment.dto.AppUserCouponVO;
import org.junit.jupiter.api.Test;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * 用户端优惠券控制器契约测试。
 */
class V1AppCouponControllerContractTest {

    @Test
    void availableCouponsShouldReturnTemplateList() throws NoSuchMethodException {
        Method method = V1AppCouponController.class.getMethod("listAvailableCoupons", Long.class);
        Type dataType = ((ParameterizedType) method.getGenericReturnType()).getActualTypeArguments()[0];
        Type itemType = ((ParameterizedType) dataType).getActualTypeArguments()[0];

        assertEquals(Result.class, ((ParameterizedType) method.getGenericReturnType()).getRawType());
        assertEquals(List.class, ((ParameterizedType) dataType).getRawType());
        assertEquals(AppCouponTemplateVO.class, itemType);
        assertEquals("/available", method.getAnnotation(GetMapping.class).value()[0]);
    }

    @Test
    void userCouponsShouldExposeOptionalStatusFilter() throws NoSuchMethodException {
        Method method = V1AppCouponController.class.getMethod("listUserCoupons", Long.class, String.class);
        Type dataType = ((ParameterizedType) method.getGenericReturnType()).getActualTypeArguments()[0];
        Type itemType = ((ParameterizedType) dataType).getActualTypeArguments()[0];
        RequestParam statusParam = method.getParameters()[1].getAnnotation(RequestParam.class);

        assertEquals(List.class, ((ParameterizedType) dataType).getRawType());
        assertEquals(AppUserCouponVO.class, itemType);
        assertEquals(false, statusParam.required());
    }

    @Test
    void receiveCouponShouldDeclareRateLimit() throws NoSuchMethodException {
        Method method = V1AppCouponController.class.getMethod("receiveCoupon", Long.class, Long.class);
        Type dataType = ((ParameterizedType) method.getGenericReturnType()).getActualTypeArguments()[0];
        RateLimit rateLimit = method.getAnnotation(RateLimit.class);

        assertEquals(AppCouponReceiveVO.class, dataType);
        assertEquals("/{templateId}/receive", method.getAnnotation(PostMapping.class).value()[0]);
        assertEquals("app:coupon:receive", rateLimit.prefix());
        assertEquals("#tenantId + ':' + #templateId", rateLimit.key());
        assertEquals(60, rateLimit.window());
        assertEquals(20, rateLimit.maxRequests());
        assertEquals(true, rateLimit.includeIp());
    }
}
