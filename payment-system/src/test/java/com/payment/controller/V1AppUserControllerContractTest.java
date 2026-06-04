package com.payment.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.payment.annotation.RateLimit;
import com.payment.dto.AppChangePasswordDTO;
import org.junit.jupiter.api.Test;
import org.springframework.web.bind.annotation.RequestParam;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 用户端个人中心控制器契约测试，锁定安全限流和分页接口形态。
 */
class V1AppUserControllerContractTest {

    /**
     * 修改密码接口Should启用限流。
     */
    @Test
    void changePasswordShouldDeclareRateLimit() throws NoSuchMethodException {
        Method method = V1AppUserController.class.getMethod("changePassword", AppChangePasswordDTO.class);

        RateLimit rateLimit = method.getAnnotation(RateLimit.class);

        assertEquals("app:user:password:change", rateLimit.prefix());
        assertEquals(300, rateLimit.window());
        assertEquals(5, rateLimit.maxRequests());
        assertTrue(rateLimit.includeIp());
    }

    /**
     * 通知列表接口Should返回分页结果并暴露分页参数。
     */
    @Test
    void listNotificationsShouldExposePageContract() throws NoSuchMethodException {
        Method method = V1AppUserController.class.getMethod("listNotifications", Integer.class, Integer.class);
        Type returnType = ((ParameterizedType) method.getGenericReturnType()).getActualTypeArguments()[0];

        assertEquals(Page.class, ((ParameterizedType) returnType).getRawType());
        assertRequestParam(method.getParameters()[0], "1");
        assertRequestParam(method.getParameters()[1], "20");
    }

    private void assertRequestParam(Parameter parameter, String defaultValue) {
        RequestParam requestParam = parameter.getAnnotation(RequestParam.class);
        assertEquals(defaultValue, requestParam.defaultValue());
    }
}
