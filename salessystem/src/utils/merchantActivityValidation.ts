import type { ActivityRuleCreatePayload } from '../types/marketing';

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

  if (draft.ruleType === 'FULL_DISCOUNT') {
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
