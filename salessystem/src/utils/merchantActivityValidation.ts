import type { ActivityRule, ActivityRuleCreatePayload, PromotionActivity } from '../types/marketing';

export type ActivityDraftValue = number | string | null | undefined;

export interface MerchantActivityDraft {
  name: string;
  startTime: string;
  endTime: string;
}

export interface MerchantActivityRuleDraft {
  ruleType: ActivityRuleCreatePayload['ruleType'];
  thresholdAmount: ActivityDraftValue;
  discountAmount: ActivityDraftValue;
  discountRate: ActivityDraftValue;
  productId: ActivityDraftValue;
  categoryCode: string;
  ruleConfigJson: string;
  priority: ActivityDraftValue;
}

export function validateMerchantActivityDraft(draft: MerchantActivityDraft) {
  const issues: string[] = [];

  if (!draft.name.trim()) {
    issues.push('请输入活动名称');
  }

  validateDateRange(draft.startTime, draft.endTime, issues);
  return issues;
}

export function validateMerchantActivityRuleDraft(draft: MerchantActivityRuleDraft) {
  const issues: string[] = [];
  const thresholdAmount = toNumber(draft.thresholdAmount);
  const discountAmount = toNumber(draft.discountAmount);
  const discountRate = toNumber(draft.discountRate);
  const productId = toNumber(draft.productId);
  const priority = toNumber(draft.priority);

  if (priority < 0) {
    issues.push('规则优先级不能小于 0');
  }

  if (draft.ruleType === 'FULL_REDUCTION') {
    if (thresholdAmount <= 0) {
      issues.push('请输入满减消费门槛');
    }
    if (discountAmount <= 0) {
      issues.push('请输入满减优惠金额');
    }
    if (thresholdAmount > 0 && discountAmount > thresholdAmount) {
      issues.push('满减优惠金额不能大于消费门槛');
    }
  }

  if (draft.ruleType === 'DISCOUNT_RATE' || draft.ruleType === 'FULL_DISCOUNT') {
    if (thresholdAmount <= 0) {
      issues.push('请输入满折消费门槛');
    }
    validateDiscountRate(discountRate, issues);
  }

  if (draft.ruleType === 'BUY_X_GET_Y') {
    if (productId <= 0) {
      issues.push('请输入买赠的商品 ID');
    }
    validateOptionalJson(draft.ruleConfigJson, '买赠高级配置必须是合法 JSON', issues);
  }

  if (draft.ruleType === 'CATEGORY_DISCOUNT') {
    if (!draft.categoryCode.trim()) {
      issues.push('请输入分类编码');
    }
    validateDiscountRate(discountRate, issues);
  }

  return issues;
}

export function detectMerchantActivityRuleConflicts(
  existingRules: ActivityRule[],
  draft: MerchantActivityRuleDraft,
) {
  const issues: string[] = [];
  const priority = toNumber(draft.priority);
  const ruleType = normalizeRuleType(draft.ruleType);
  const thresholdAmount = toNumber(draft.thresholdAmount);
  const productId = toNumber(draft.productId);
  const categoryCode = draft.categoryCode.trim().toLowerCase();

  if (existingRules.some((rule) => Number(rule.priority ?? 0) === priority)) {
    issues.push('已有相同优先级的活动规则，可能导致用户结算时命中顺序不清晰');
  }

  if (ruleType === 'FULL_REDUCTION' && existingRules.some((rule) =>
    normalizeRuleType(rule.ruleType) === 'FULL_REDUCTION'
    && Number(rule.thresholdAmount ?? 0) === thresholdAmount,
  )) {
    issues.push('已有相同门槛的满减规则，请调整门槛或合并规则');
  }

  if (ruleType === 'DISCOUNT_RATE' && existingRules.some((rule) =>
    normalizeRuleType(rule.ruleType) === 'DISCOUNT_RATE'
    && Number(rule.thresholdAmount ?? 0) === thresholdAmount,
  )) {
    issues.push('已有相同门槛的满折规则，请调整门槛或合并规则');
  }

  if (ruleType === 'BUY_X_GET_Y' && productId > 0 && existingRules.some((rule) =>
    normalizeRuleType(rule.ruleType) === 'BUY_X_GET_Y'
    && Number(rule.productId ?? 0) === productId,
  )) {
    issues.push('该商品已有买赠规则，请避免重复发放权益');
  }

  if (ruleType === 'CATEGORY_DISCOUNT' && categoryCode && existingRules.some((rule) =>
    normalizeRuleType(rule.ruleType) === 'CATEGORY_DISCOUNT'
    && String(rule.categoryCode ?? '').trim().toLowerCase() === categoryCode,
  )) {
    issues.push('该分类已有折扣规则，请避免同一分类重复配置');
  }

  return issues;
}

export function detectMerchantCrossActivityRuleConflicts(
  activities: PromotionActivity[],
  activityRules: Record<number, ActivityRule[]>,
  targetActivity: Pick<PromotionActivity, 'id' | 'startTime' | 'endTime' | 'status'>,
  draft: MerchantActivityRuleDraft,
) {
  const issues: string[] = [];
  const priority = toNumber(draft.priority);
  const ruleType = normalizeRuleType(draft.ruleType);
  const thresholdAmount = toNumber(draft.thresholdAmount);
  const productId = toNumber(draft.productId);
  const categoryCode = draft.categoryCode.trim().toLowerCase();

  const overlappingActivities = activities.filter((activity) => (
    activity.id !== targetActivity.id
    && (activity.status === 'DRAFT' || activity.status === 'ACTIVE')
    && hasOverlappingWindow(targetActivity.startTime, targetActivity.endTime, activity.startTime, activity.endTime)
  ));

  const existingRules = overlappingActivities.flatMap((activity) => activityRules[activity.id] ?? []);
  if (existingRules.length === 0) {
    return issues;
  }

  if (existingRules.some((rule) => Number(rule.priority ?? 0) === priority)) {
    issues.push('存在其他活动使用相同优先级的规则，请避免跨活动命中顺序不清晰');
  }

  if (ruleType === 'FULL_REDUCTION' && existingRules.some((rule) =>
    normalizeRuleType(rule.ruleType) === 'FULL_REDUCTION'
    && Number(rule.thresholdAmount ?? 0) === thresholdAmount,
  )) {
    issues.push('存在其他活动配置了相同门槛的满减规则');
  }

  if (ruleType === 'DISCOUNT_RATE' && existingRules.some((rule) =>
    normalizeRuleType(rule.ruleType) === 'DISCOUNT_RATE'
    && Number(rule.thresholdAmount ?? 0) === thresholdAmount,
  )) {
    issues.push('存在其他活动配置了相同门槛的满折规则');
  }

  if (ruleType === 'BUY_X_GET_Y' && productId > 0 && existingRules.some((rule) =>
    normalizeRuleType(rule.ruleType) === 'BUY_X_GET_Y'
    && Number(rule.productId ?? 0) === productId,
  )) {
    issues.push('存在其他活动使用同一商品配置买赠规则');
  }

  if (ruleType === 'CATEGORY_DISCOUNT' && categoryCode && existingRules.some((rule) =>
    normalizeRuleType(rule.ruleType) === 'CATEGORY_DISCOUNT'
    && String(rule.categoryCode ?? '').trim().toLowerCase() === categoryCode,
  )) {
    issues.push('存在其他活动使用同一分类配置折扣规则');
  }

  return issues;
}

export function normalizeRuleType(ruleType: ActivityRuleCreatePayload['ruleType'] | string) {
  return ruleType === 'FULL_DISCOUNT' ? 'DISCOUNT_RATE' : ruleType;
}

function hasOverlappingWindow(
  leftStartTime: string,
  leftEndTime: string,
  rightStartTime: string,
  rightEndTime: string,
) {
  const leftStart = parseDateTime(leftStartTime);
  const leftEnd = parseDateTime(leftEndTime);
  const rightStart = parseDateTime(rightStartTime);
  const rightEnd = parseDateTime(rightEndTime);
  if (!leftStart || !leftEnd || !rightStart || !rightEnd) {
    return false;
  }

  return leftStart.getTime() <= rightEnd.getTime() && rightStart.getTime() <= leftEnd.getTime();
}

function validateDiscountRate(discountRate: number, issues: string[]) {
  if (discountRate <= 0 || discountRate >= 1) {
    issues.push('折扣比例必须大于 0 且小于 1');
  }
}

function validateDateRange(startTime: string, endTime: string, issues: string[]) {
  if (!startTime || !endTime) {
    issues.push('请选择活动开始与结束时间');
    return;
  }

  const start = parseDateTime(startTime);
  const end = parseDateTime(endTime);
  if (!start || !end || end.getTime() <= start.getTime()) {
    issues.push('活动结束时间必须晚于开始时间');
  }
}

function validateOptionalJson(value: string, message: string, issues: string[]) {
  if (!value.trim()) {
    return;
  }

  try {
    JSON.parse(value);
  } catch {
    issues.push(message);
  }
}

function toNumber(value: ActivityDraftValue) {
  if (value === '' || value == null) {
    return 0;
  }
  const amount = Number(value);
  return Number.isFinite(amount) ? amount : 0;
}

function parseDateTime(value: string) {
  const date = new Date(value);
  return Number.isNaN(date.getTime()) ? null : date;
}
