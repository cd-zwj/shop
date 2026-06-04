package com.payment.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.payment.common.Result;
import com.payment.dto.ActivityRuleCreateDTO;
import com.payment.dto.CouponScopeCreateDTO;
import com.payment.dto.CouponTemplateCreateDTO;
import com.payment.dto.PromotionActivityCreateDTO;
import com.payment.entity.ActivityRule;
import com.payment.entity.CouponScope;
import com.payment.entity.CouponTemplate;
import com.payment.entity.PromotionActivity;
import org.junit.jupiter.api.Test;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * 管理端营销运营控制器契约测试。
 */
class V1AdminMarketingControllerContractTest {

    @Test
    void controllerShouldUseAdminMarketingBasePath() {
        RequestMapping mapping = V1AdminMarketingController.class.getAnnotation(RequestMapping.class);

        assertNotNull(mapping);
        assertEquals("/v1/admin/marketing", mapping.value()[0]);
    }

    @Test
    void platformCouponEndpointsShouldExposeAdminContract() throws NoSuchMethodException {
        Method list = V1AdminMarketingController.class.getMethod("listPlatformCouponTemplates", String.class);
        Method create = V1AdminMarketingController.class.getMethod("createPlatformCouponTemplate", CouponTemplateCreateDTO.class);
        Method activate = V1AdminMarketingController.class.getMethod("activateCouponTemplate", Long.class);

        assertListResult(list, CouponTemplate.class);
        assertEquals("/coupons", list.getAnnotation(GetMapping.class).value()[0]);
        assertPermission(list, "admin:marketing:list");
        assertResultData(create, CouponTemplate.class);
        assertEquals("/coupons", create.getAnnotation(PostMapping.class).value()[0]);
        assertPermission(create, "admin:marketing:create");
        assertEquals("/coupons/{templateId}/activate", activate.getAnnotation(PutMapping.class).value()[0]);
    }

    @Test
    void platformCouponScopeEndpointsShouldExposeAdminContract() throws NoSuchMethodException {
        Method list = V1AdminMarketingController.class.getMethod("listCouponScopes", Long.class);
        Method create = V1AdminMarketingController.class.getMethod("addCouponScope", Long.class, CouponScopeCreateDTO.class);

        assertListResult(list, CouponScope.class);
        assertEquals("/coupons/{templateId}/scopes", list.getAnnotation(GetMapping.class).value()[0]);
        assertResultData(create, CouponScope.class);
        assertEquals("/coupons/{templateId}/scopes", create.getAnnotation(PostMapping.class).value()[0]);
    }

    @Test
    void platformActivityEndpointsShouldExposeAdminContract() throws NoSuchMethodException {
        Method list = V1AdminMarketingController.class.getMethod("listPlatformActivities", String.class);
        Method create = V1AdminMarketingController.class.getMethod("createPlatformActivity", PromotionActivityCreateDTO.class);
        Method addRule = V1AdminMarketingController.class.getMethod("addActivityRule", Long.class, ActivityRuleCreateDTO.class);

        assertListResult(list, PromotionActivity.class);
        assertEquals("/activities", list.getAnnotation(GetMapping.class).value()[0]);
        assertPermission(list, "admin:marketing:list");
        assertResultData(create, PromotionActivity.class);
        assertEquals("/activities", create.getAnnotation(PostMapping.class).value()[0]);
        assertPermission(create, "admin:marketing:create");
        assertResultData(addRule, ActivityRule.class);
        assertEquals("/activities/{activityId}/rules", addRule.getAnnotation(PostMapping.class).value()[0]);
    }

    private void assertPermission(Method method, String permission) {
        SaCheckPermission annotation = method.getAnnotation(SaCheckPermission.class);
        assertNotNull(annotation);
        assertEquals(permission, annotation.value()[0]);
    }

    private void assertListResult(Method method, Class<?> itemType) {
        Type dataType = ((ParameterizedType) method.getGenericReturnType()).getActualTypeArguments()[0];
        assertEquals(Result.class, ((ParameterizedType) method.getGenericReturnType()).getRawType());
        assertEquals(List.class, ((ParameterizedType) dataType).getRawType());
        assertEquals(itemType, ((ParameterizedType) dataType).getActualTypeArguments()[0]);
    }

    private void assertResultData(Method method, Class<?> dataClass) {
        Type dataType = ((ParameterizedType) method.getGenericReturnType()).getActualTypeArguments()[0];
        assertEquals(Result.class, ((ParameterizedType) method.getGenericReturnType()).getRawType());
        assertEquals(dataClass, dataType);
    }
}
