package com.payment.controller;

import com.payment.common.PageResult;
import com.payment.common.Result;
import com.payment.dto.MerchantTransactionVO;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;

import static org.junit.jupiter.api.Assertions.assertEquals;

class V1MerchantFinanceControllerContractTest {

    @Test
    void transactionsShouldReturnPageResultInsteadOfRawMybatisPage() throws NoSuchMethodException {
        Method method = V1MerchantFinanceController.class.getMethod(
                "listTransactions",
                Long.class,
                String.class,
                String.class,
                String.class,
                Integer.class,
                Integer.class);

        ParameterizedType resultType = (ParameterizedType) method.getGenericReturnType();
        assertEquals(Result.class, resultType.getRawType());

        ParameterizedType pageType = (ParameterizedType) resultType.getActualTypeArguments()[0];
        assertEquals(PageResult.class, pageType.getRawType());
        assertEquals(MerchantTransactionVO.class, pageType.getActualTypeArguments()[0]);
    }
}
