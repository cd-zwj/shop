package com.payment.controller;

import cn.dev33.satoken.annotation.SaCheckLogin;
import com.payment.dto.AppUserVO;
import org.junit.jupiter.api.Test;
import org.springframework.web.bind.annotation.GetMapping;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 用户端个人中心控制器契约测试，锁定当前用户信息接口的返回结构和安全要求。
 */
class V1AppUserControllerContractTest {

    @Test
    void getCurrentUserShouldReturnAppUserVOWithSecurityAnnotation() throws NoSuchMethodException {
        Method method = V1AppUserController.class.getMethod("getCurrentUser");

        assertNotNull(method.getAnnotation(SaCheckLogin.class), "getCurrentUser must require login");
        assertNotNull(method.getAnnotation(GetMapping.class), "getCurrentUser must be mapped");
        assertEquals("/me", method.getAnnotation(GetMapping.class).value()[0]);

        Type returnType = method.getGenericReturnType();
        assertTrue(returnType instanceof ParameterizedType, "return type should be parameterized");

        ParameterizedType parameterizedType = (ParameterizedType) returnType;
        assertEquals(com.payment.common.Result.class, parameterizedType.getRawType());

        Type innerType = parameterizedType.getActualTypeArguments()[0];
        assertEquals(AppUserVO.class, innerType);
    }

    @Test
    void appUserVOSafeViewShouldNotExposeSensitiveFields() {
        Set<String> fieldNames = Arrays.stream(AppUserVO.class.getDeclaredFields())
                .map(Field::getName)
                .collect(Collectors.toSet());

        assertTrue(fieldNames.contains("id"), "AppUserVO should expose id");
        assertTrue(fieldNames.contains("username"), "AppUserVO should expose username");

        assertTrue(fieldNames.stream().noneMatch(name -> name.equalsIgnoreCase("passwordHash")),
                "AppUserVO must not expose passwordHash");
        assertTrue(fieldNames.stream().noneMatch(name -> name.equalsIgnoreCase("deleted")),
                "AppUserVO must not expose deleted");
        assertTrue(fieldNames.stream().noneMatch(name -> name.equalsIgnoreCase("updateTime")),
                "AppUserVO must not expose updateTime");
    }

    @Test
    void toVOShouldReturnNullWhenGivenNullEntity() {
        assertNull(AppUserVO.toVO(null));
    }
}
