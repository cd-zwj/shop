export interface MerchantCouponTemplate {
  id: number;
  tenantId: number | null;
  ownerType: 'PLATFORM' | 'TENANT';
  name: string;
  couponType: 'FIXED' | 'RATE';
  thresholdAmount: number | null;
  discountAmount: number | null;
  discountRate: number | null;
  maxDiscountAmount: number | null;
  totalStock: number;
  perUserLimit: number;
  status: string; // DRAFT | ACTIVE | DISABLED
  receiveStartTime: string | null;
  receiveEndTime: string | null;
  validStartTime: string | null;
  validEndTime: string | null;
  validDaysAfterReceive: number | null;
  description: string | null;
  stackStrategy: string | null;
  requiredMemberLevel?: number | null;
  requiredMemberTagIds?: string | null;
  excludedMemberTagIds?: string | null;
  receivedQuantity?: number | null;
  usedQuantity?: number | null;
  createTime: string;
}

export interface CouponTemplateCreatePayload {
  name: string;
  couponType: 'FIXED' | 'RATE';
  thresholdAmount?: number;
  discountAmount?: number;
  discountRate?: number;
  maxDiscountAmount?: number;
  totalStock: number;
  perUserLimit?: number;
  receiveStartTime?: string;
  receiveEndTime?: string;
  validStartTime?: string;
  validEndTime?: string;
  validDaysAfterReceive?: number;
  description?: string;
  stackStrategy?: string;
  requiredMemberLevel?: number;
  requiredMemberTagIds?: string;
  excludedMemberTagIds?: string;
  ownerType?: 'PLATFORM' | 'TENANT';
  tenantId?: number;
}

export interface CouponScope {
  id: number;
  couponTemplateId: number;
  scopeType: string; // ALL | CATEGORY | PRODUCT
  scopeId?: number | null;
  scopeCode?: string | null;
  tenantId?: number | null;
}

export interface CouponScopeCreatePayload {
  scopeType: string;
  scopeId?: number;
  scopeCode?: string;
  tenantId?: number;
}

export type ActivityRuleType = 'FULL_REDUCTION' | 'DISCOUNT_RATE' | 'FULL_DISCOUNT' | 'BUY_X_GET_Y' | 'CATEGORY_DISCOUNT';

export interface PromotionActivity {
  id: number;
  tenantId: number | null;
  ownerType: 'PLATFORM' | 'TENANT';
  name: string;
  activityType: ActivityRuleType | string;
  startTime: string;
  endTime: string;
  status: string; // DRAFT | ACTIVE | DISABLED
  description?: string | null;
  createTime?: string | null;
}

export interface ActivityRule {
  id: number;
  activityId: number;
  ruleType: ActivityRuleType;
  thresholdAmount?: number | null;
  discountAmount?: number | null;
  discountRate?: number | null;
  productId?: number | null;
  categoryCode?: string | null;
  ruleConfigJson?: string | null;
  priority: number;
}

export interface ActivityRuleCreatePayload {
  ruleType: ActivityRuleType;
  thresholdAmount?: number;
  discountAmount?: number;
  discountRate?: number;
  productId?: number;
  categoryCode?: string;
  ruleConfigJson?: string;
  priority?: number;
}

export interface MemberLevel {
  id: number;
  tenantId: number;
  level: number;
  name: string;
  thresholdAmount: number;
  discountRate: number;
}

export interface MemberTag {
  id: number;
  tenantId: number;
  name: string;
  memberCount?: number;
}

export interface MarketingEffectSummary {
  templateCount: number;
  activeTemplateCount: number;
  receivedCount: number;
  usedCount: number;
  remainingStock: number;
  writeOffRate: number;
  activityCount?: number;
  activeActivityCount?: number;
  activityDiscountAmount?: number;
}

export interface CouponEffect {
  templateId: number;
  templateName: string;
  totalQuantity: number;
  receivedCount: number;
  usedCount: number;
  remainingStock: number;
  writeOffRate: number;
}
