import { request } from '../request';
import type {
  MerchantCouponTemplate,
  CouponTemplateCreatePayload,
  CouponScope,
  CouponScopeCreatePayload,
  PromotionActivity,
  ActivityRule,
  ActivityRuleCreatePayload,
  ActivityRuleType,
  MemberLevel,
  MemberTag,
  MarketingEffectSummary,
  CouponEffect,
} from '../../types/marketing';

export const merchantMarketingService = {
  getEffectSummary(tenantId: number) {
    return request<MarketingEffectSummary>({
      url: `/v1/merchant/tenants/${tenantId}/marketing/effect/summary`,
      method: 'get',
      authRole: 'merchant',
    });
  },

  getCouponEffect(tenantId: number, templateId: number) {
    return request<CouponEffect>({
      url: `/v1/merchant/tenants/${tenantId}/marketing/coupons/${templateId}/effect`,
      method: 'get',
      authRole: 'merchant',
    });
  },

  // 优惠券模板
  getCouponTemplates(tenantId: number, status?: string) {
    return request<MerchantCouponTemplate[]>({
      url: `/v1/merchant/tenants/${tenantId}/marketing/coupons`,
      method: 'get',
      params: { status },
      authRole: 'merchant',
    });
  },

  createCouponTemplate(tenantId: number, payload: CouponTemplateCreatePayload) {
    return request<MerchantCouponTemplate>({
      url: `/v1/merchant/tenants/${tenantId}/marketing/coupons`,
      method: 'post',
      data: normalizeCouponCreatePayload(payload),
      authRole: 'merchant',
    });
  },

  activateCoupon(tenantId: number, templateId: number) {
    return request<void>({
      url: `/v1/merchant/tenants/${tenantId}/marketing/coupons/${templateId}/activate`,
      method: 'put',
      authRole: 'merchant',
    });
  },

  disableCoupon(tenantId: number, templateId: number) {
    return request<void>({
      url: `/v1/merchant/tenants/${tenantId}/marketing/coupons/${templateId}/disable`,
      method: 'put',
      authRole: 'merchant',
    });
  },

  getCouponScopes(tenantId: number, templateId: number) {
    return request<CouponScope[]>({
      url: `/v1/merchant/tenants/${tenantId}/marketing/coupons/${templateId}/scopes`,
      method: 'get',
      authRole: 'merchant',
    });
  },

  addCouponScope(tenantId: number, templateId: number, payload: CouponScopeCreatePayload) {
    return request<CouponScope>({
      url: `/v1/merchant/tenants/${tenantId}/marketing/coupons/${templateId}/scopes`,
      method: 'post',
      data: payload,
      authRole: 'merchant',
    });
  },

  // 营销活动
  getActivities(tenantId: number, status?: string) {
    return request<BackendPromotionActivity[]>({
      url: `/v1/merchant/tenants/${tenantId}/marketing/activities`,
      method: 'get',
      params: { status },
      authRole: 'merchant',
    }).then((activities) => activities.map(normalizeActivity));
  },

  createActivity(tenantId: number, payload: { name: string; activityType: string; startTime: string; endTime: string; description?: string }) {
    return request<BackendPromotionActivity>({
      url: `/v1/merchant/tenants/${tenantId}/marketing/activities`,
      method: 'post',
      data: {
        activityName: payload.name,
        activityType: normalizeActivityType(payload.activityType),
        startTime: payload.startTime,
        endTime: payload.endTime,
        description: payload.description,
      },
      authRole: 'merchant',
    }).then(normalizeActivity);
  },

  getActivityRules(tenantId: number, activityId: number) {
    return request<BackendActivityRule[]>({
      url: `/v1/merchant/tenants/${tenantId}/marketing/activities/${activityId}/rules`,
      method: 'get',
      authRole: 'merchant',
    }).then((rules) => rules.map(normalizeActivityRule));
  },

  addActivityRule(tenantId: number, activityId: number, payload: ActivityRuleCreatePayload) {
    return request<BackendActivityRule>({
      url: `/v1/merchant/tenants/${tenantId}/marketing/activities/${activityId}/rules`,
      method: 'post',
      data: {
        ...payload,
        ruleType: normalizeActivityType(payload.ruleType),
      },
      authRole: 'merchant',
    }).then(normalizeActivityRule);
  },

  activateActivity(tenantId: number, activityId: number) {
    return request<void>({
      url: `/v1/merchant/tenants/${tenantId}/marketing/activities/${activityId}/activate`,
      method: 'put',
      authRole: 'merchant',
    });
  },

  disableActivity(tenantId: number, activityId: number) {
    return request<void>({
      url: `/v1/merchant/tenants/${tenantId}/marketing/activities/${activityId}/disable`,
      method: 'put',
      authRole: 'merchant',
    });
  },

  // 会员等级与标签
  getMemberLevels(tenantId: number) {
    return request<MemberLevel[]>({
      url: `/v1/merchant/tenants/${tenantId}/marketing/member-levels`,
      method: 'get',
      authRole: 'merchant',
    });
  },

  createMemberLevel(tenantId: number, params: { level: number; name: string; thresholdAmount: number; discountRate: number }) {
    return request<MemberLevel>({
      url: `/v1/merchant/tenants/${tenantId}/marketing/member-levels`,
      method: 'post',
      params, // 后端使用 @RequestParam 接收参数
      authRole: 'merchant',
    });
  },

  getMemberTags(tenantId: number) {
    return request<MemberTag[]>({
      url: `/v1/merchant/tenants/${tenantId}/marketing/member-tags`,
      method: 'get',
      authRole: 'merchant',
    });
  },

  createMemberTag(tenantId: number, name: string) {
    return request<MemberTag>({
      url: `/v1/merchant/tenants/${tenantId}/marketing/member-tags`,
      method: 'post',
      params: { name }, // 后端使用 @RequestParam 接收参数
      authRole: 'merchant',
    });
  },
};

interface BackendPromotionActivity extends Omit<PromotionActivity, 'name' | 'ownerType'> {
  activityName?: string | null;
  activityScope?: 'PLATFORM' | 'TENANT' | string | null;
  name?: string;
  ownerType?: 'PLATFORM' | 'TENANT';
}

type BackendActivityRule = ActivityRule;

function normalizeActivity(activity: BackendPromotionActivity): PromotionActivity {
  return {
    ...activity,
    ownerType: normalizeOwnerType(activity.ownerType ?? activity.activityScope),
    name: activity.name ?? activity.activityName ?? '',
    activityType: normalizeActivityType(activity.activityType),
  };
}

function normalizeActivityRule(rule: BackendActivityRule): ActivityRule {
  return {
    ...rule,
    ruleType: normalizeActivityType(rule.ruleType),
  };
}

function normalizeOwnerType(ownerType?: string | null): 'PLATFORM' | 'TENANT' {
  return ownerType === 'PLATFORM' ? 'PLATFORM' : 'TENANT';
}

function normalizeActivityType(activityType: string): ActivityRuleType {
  return activityType === 'FULL_DISCOUNT' ? 'DISCOUNT_RATE' : activityType as ActivityRuleType;
}

function normalizeCouponCreatePayload(payload: CouponTemplateCreatePayload) {
  return {
    templateName: payload.name,
    couponType: payload.couponType === 'RATE' ? 'DISCOUNT_RATE' : 'FULL_REDUCTION',
    thresholdAmount: payload.thresholdAmount,
    discountAmount: payload.discountAmount,
    discountRate: payload.discountRate,
    maxDiscountAmount: payload.maxDiscountAmount,
    totalQuantity: payload.totalStock,
    perUserLimit: payload.perUserLimit,
    receiveStartTime: payload.receiveStartTime,
    receiveEndTime: payload.receiveEndTime,
    validStartTime: payload.validStartTime,
    validEndTime: payload.validEndTime,
    validDays: payload.validDaysAfterReceive,
    description: payload.description,
    requiredMemberLevel: payload.requiredMemberLevel,
    requiredMemberTagIds: payload.requiredMemberTagIds,
    excludedMemberTagIds: payload.excludedMemberTagIds,
  };
}
