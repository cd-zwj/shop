package com.payment.controller;

import com.payment.common.Result;
import com.payment.dto.ActivityRuleCreateDTO;
import com.payment.dto.CouponScopeCreateDTO;
import com.payment.dto.CouponTemplateCreateDTO;
import com.payment.dto.PromotionActivityCreateDTO;
import com.payment.entity.ActivityRule;
import com.payment.entity.CouponScope;
import com.payment.entity.CouponTemplate;
import com.payment.entity.MemberLevel;
import com.payment.entity.MemberTag;
import com.payment.entity.PromotionActivity;
import org.junit.jupiter.api.Test;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * 商户端营销运营控制器契约测试。
 */
class V1MerchantMarketingControllerContractTest {

    @Test
    void controllerShouldUseMerchantMarketingBasePath() {
        RequestMapping mapping = V1MerchantMarketingController.class.getAnnotation(RequestMapping.class);

        assertNotNull(mapping);
        assertEquals("/v1/merchant/tenants/{tenantId}/marketing", mapping.value()[0]);
    }

    @Test
    void couponTemplateEndpointsShouldExposeManagementContract() throws NoSuchMethodException {
        Method list = V1MerchantMarketingController.class.getMethod("listCouponTemplates", Long.class, String.class);
        Method create = V1MerchantMarketingController.class.getMethod("createCouponTemplate", Long.class, CouponTemplateCreateDTO.class);
        Method activate = V1MerchantMarketingController.class.getMethod("activateCouponTemplate", Long.class, Long.class);

        assertListResult(list, CouponTemplate.class);
        assertEquals("/coupons", list.getAnnotation(GetMapping.class).value()[0]);
        assertResultData(create, CouponTemplate.class);
        assertEquals("/coupons", create.getAnnotation(PostMapping.class).value()[0]);
        assertEquals("/coupons/{templateId}/activate", activate.getAnnotation(PutMapping.class).value()[0]);
    }

    @Test
    void couponScopeEndpointShouldExposeManagementContract() throws NoSuchMethodException {
        Method create = V1MerchantMarketingController.class.getMethod("addCouponScope", Long.class, Long.class, CouponScopeCreateDTO.class);
        PathVariable templateId = create.getParameters()[1].getAnnotation(PathVariable.class);

        assertResultData(create, CouponScope.class);
        assertEquals("/coupons/{templateId}/scopes", create.getAnnotation(PostMapping.class).value()[0]);
        assertEquals("templateId", templateId.value());
    }

    @Test
    void activityEndpointsShouldExposeManagementContract() throws NoSuchMethodException {
        Method list = V1MerchantMarketingController.class.getMethod("listActivities", Long.class, String.class);
        Method create = V1MerchantMarketingController.class.getMethod("createActivity", Long.class, PromotionActivityCreateDTO.class);
        Method addRule = V1MerchantMarketingController.class.getMethod("addActivityRule", Long.class, Long.class, ActivityRuleCreateDTO.class);

        assertListResult(list, PromotionActivity.class);
        assertEquals("/activities", list.getAnnotation(GetMapping.class).value()[0]);
        assertResultData(create, PromotionActivity.class);
        assertEquals("/activities", create.getAnnotation(PostMapping.class).value()[0]);
        assertResultData(addRule, ActivityRule.class);
        assertEquals("/activities/{activityId}/rules", addRule.getAnnotation(PostMapping.class).value()[0]);
    }

    @Test
    void memberOperationEndpointsShouldExposeManagementContract() throws NoSuchMethodException {
        Method listLevels = V1MerchantMarketingController.class.getMethod("listMemberLevels", Long.class);
        Method createLevel = V1MerchantMarketingController.class.getMethod("createMemberLevel", Long.class, Integer.class, String.class, BigDecimal.class, BigDecimal.class);
        Method assignTag = V1MerchantMarketingController.class.getMethod("assignMemberTag", Long.class, Long.class, Long.class);

        assertListResult(listLevels, MemberLevel.class);
        assertEquals("/member-levels", listLevels.getAnnotation(GetMapping.class).value()[0]);
        assertResultData(createLevel, MemberLevel.class);
        assertEquals("/member-levels", createLevel.getAnnotation(PostMapping.class).value()[0]);
        assertEquals("/members/{memberId}/tags/{tagId}", assignTag.getAnnotation(PutMapping.class).value()[0]);
    }

    @Test
    void memberTagEndpointsShouldExposeManagementContract() throws NoSuchMethodException {
        Method listTags = V1MerchantMarketingController.class.getMethod("listMemberTags", Long.class);
        Method createTag = V1MerchantMarketingController.class.getMethod("createMemberTag", Long.class, String.class);

        assertListResult(listTags, MemberTag.class);
        assertEquals("/member-tags", listTags.getAnnotation(GetMapping.class).value()[0]);
        assertResultData(createTag, MemberTag.class);
        assertEquals("/member-tags", createTag.getAnnotation(PostMapping.class).value()[0]);
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
