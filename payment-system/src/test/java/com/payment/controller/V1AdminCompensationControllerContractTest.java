package com.payment.controller;

import com.payment.common.PageResult;
import com.payment.common.Result;
import com.payment.dto.CompensationTaskVO;
import com.payment.dto.RetryTaskVO;
import com.payment.entity.CompensationTask;
import com.payment.entity.RetryTask;
import org.junit.jupiter.api.Test;
import org.springframework.web.bind.annotation.GetMapping;

import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

import static org.junit.jupiter.api.Assertions.assertEquals;

class V1AdminCompensationControllerContractTest {

    @Test
    void compensationTaskListShouldReturnPageResultVo() throws NoSuchMethodException {
        Method method = V1AdminCompensationController.class.getMethod(
                "listCompensationTasks", String.class, String.class, Integer.class, Integer.class);

        assertEquals("/compensation-tasks", method.getAnnotation(GetMapping.class).value()[0]);
        assertPageResultData(method, CompensationTaskVO.class);
    }

    @Test
    void retryTaskListShouldReturnPageResultVo() throws NoSuchMethodException {
        Method method = V1AdminCompensationController.class.getMethod(
                "listRetryTasks", String.class, String.class, Integer.class, Integer.class);

        assertEquals("/retry-tasks", method.getAnnotation(GetMapping.class).value()[0]);
        assertPageResultData(method, RetryTaskVO.class);
    }

    @Test
    void adminCompensationControllerShouldNotExposeEntitiesDirectly() {
        for (Method method : V1AdminCompensationController.class.getDeclaredMethods()) {
            Type returnType = method.getGenericReturnType();
            String typeName = returnType.getTypeName();
            if (typeName.contains(CompensationTask.class.getName())
                    || typeName.contains(RetryTask.class.getName())) {
                throw new AssertionError("Admin compensation endpoint exposes entity directly: " + method.getName());
            }
        }
    }

    private void assertPageResultData(Method method, Class<?> itemType) {
        ParameterizedType resultType = (ParameterizedType) method.getGenericReturnType();
        assertEquals(Result.class, resultType.getRawType());
        ParameterizedType pageResultType = (ParameterizedType) resultType.getActualTypeArguments()[0];
        assertEquals(PageResult.class, pageResultType.getRawType());
        assertEquals(itemType, pageResultType.getActualTypeArguments()[0]);
    }
}
