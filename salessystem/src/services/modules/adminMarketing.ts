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
} from '../../types/marketing';

export const adminMarketingService = {
  // Coupon Templates
  getCouponTemplates(status?: string) {
    return request<MerchantCouponTemplate[]>({
      url: `/v1/admin/marketing/coupons`,
      method: 'get',
      params: { status },
      authRole: 'admin',
    });
  },

  createCouponTemplate(payload: CouponTemplateCreatePayload) {
    return request<MerchantCouponTemplate>({
      url: `/v1/admin/marketing/coupons`,
      method: 'post',
      data: payload,
      authRole: 'admin',
    });
  },

  activateCoupon(templateId: number) {
    return request<void>({
      url: `/v1/admin/marketing/coupons/${templateId}/activate`,
      method: 'put',
      authRole: 'admin',
    });
  },

  disableCoupon(templateId: number) {
    return request<void>({
      url: `/v1/admin/marketing/coupons/${templateId}/disable`,
      method: 'put',
      authRole: 'admin',
    });
  },

  getCouponScopes(templateId: number) {
    return request<CouponScope[]>({
      url: `/v1/admin/marketing/coupons/${templateId}/scopes`,
      method: 'get',
      authRole: 'admin',
    });
  },

  addCouponScope(templateId: number, payload: CouponScopeCreatePayload) {
    return request<CouponScope>({
      url: `/v1/admin/marketing/coupons/${templateId}/scopes`,
      method: 'post',
      data: payload,
      authRole: 'admin',
    });
  },

  // Promotion Activities
  getActivities(status?: string) {
    return request<BackendPromotionActivity[]>({
      url: `/v1/admin/marketing/activities`,
      method: 'get',
      params: { status },
      authRole: 'admin',
    }).then((activities) => activities.map(normalizeActivity));
  },

  createActivity(payload: { name: string; activityType: string; startTime: string; endTime: string; description?: string }) {
    return request<BackendPromotionActivity>({
      url: `/v1/admin/marketing/activities`,
      method: 'post',
      data: {
        activityName: payload.name,
        activityType: normalizeActivityType(payload.activityType),
        startTime: payload.startTime,
        endTime: payload.endTime,
        description: payload.description,
      },
      authRole: 'admin',
    }).then(normalizeActivity);
  },

  getActivityRules(activityId: number) {
    return request<BackendActivityRule[]>({
      url: `/v1/admin/marketing/activities/${activityId}/rules`,
      method: 'get',
      authRole: 'admin',
    }).then((rules) => rules.map(normalizeActivityRule));
  },

  addActivityRule(activityId: number, payload: ActivityRuleCreatePayload) {
    return request<BackendActivityRule>({
      url: `/v1/admin/marketing/activities/${activityId}/rules`,
      method: 'post',
      data: {
        ...payload,
        ruleType: normalizeActivityType(payload.ruleType),
      },
      authRole: 'admin',
    }).then(normalizeActivityRule);
  },

  activateActivity(activityId: number) {
    return request<void>({
      url: `/v1/admin/marketing/activities/${activityId}/activate`,
      method: 'put',
      authRole: 'admin',
    });
  },

  disableActivity(activityId: number) {
    return request<void>({
      url: `/v1/admin/marketing/activities/${activityId}/disable`,
      method: 'put',
      authRole: 'admin',
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
  return ownerType === 'TENANT' ? 'TENANT' : 'PLATFORM';
}

function normalizeActivityType(activityType: string): ActivityRuleType {
  return activityType === 'FULL_DISCOUNT' ? 'DISCOUNT_RATE' : activityType as ActivityRuleType;
}
