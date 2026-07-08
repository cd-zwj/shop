import { describe, expect, it } from 'vitest';
import {
  detectMerchantActivityRuleConflicts,
  validateMerchantActivityDraft,
  validateMerchantActivityRuleDraft,
} from './merchantActivityValidation';

const validActivityDraft = {
  name: '618 年中促销',
  startTime: '2026-08-01T10:00',
  endTime: '2026-08-10T10:00',
};

const validRuleDraft = {
  ruleType: 'FULL_REDUCTION' as const,
  thresholdAmount: 100,
  discountAmount: 20,
  discountRate: '',
  productId: '',
  categoryCode: '',
  ruleConfigJson: '',
  priority: 0,
};

describe('validateMerchantActivityDraft', () => {
  it('accepts a valid activity window', () => {
    expect(validateMerchantActivityDraft(validActivityDraft)).toEqual([]);
  });

  it('blocks blank names and inverted activity windows', () => {
    expect(validateMerchantActivityDraft({
      name: '  ',
      startTime: '2026-08-10T10:00',
      endTime: '2026-08-01T10:00',
    })).toEqual(expect.arrayContaining([
      '请输入活动名称',
      '活动结束时间必须晚于开始时间',
    ]));
  });
});

describe('validateMerchantActivityRuleDraft', () => {
  it('accepts a valid full reduction rule', () => {
    expect(validateMerchantActivityRuleDraft(validRuleDraft)).toEqual([]);
  });

  it('blocks full reduction discounts greater than threshold', () => {
    expect(validateMerchantActivityRuleDraft({
      ...validRuleDraft,
      discountAmount: 120,
    })).toContain('满减优惠金额不能大于消费门槛');
  });

  it('blocks invalid discount rates', () => {
    expect(validateMerchantActivityRuleDraft({
      ...validRuleDraft,
      ruleType: 'FULL_DISCOUNT',
      discountAmount: '',
      discountRate: 1,
    })).toContain('折扣比例必须大于 0 且小于 1');
  });

  it('blocks malformed buy-x-get-y json config', () => {
    expect(validateMerchantActivityRuleDraft({
      ...validRuleDraft,
      ruleType: 'BUY_X_GET_Y',
      thresholdAmount: '',
      discountAmount: '',
      productId: 7,
      ruleConfigJson: '{bad json',
    })).toContain('买赠高级配置必须是合法 JSON');
  });

  it('blocks missing category codes for category discount rules', () => {
    expect(validateMerchantActivityRuleDraft({
      ...validRuleDraft,
      ruleType: 'CATEGORY_DISCOUNT',
      thresholdAmount: '',
      discountAmount: '',
      discountRate: 0.8,
      categoryCode: '  ',
    })).toContain('请输入分类编码');
  });
});

describe('detectMerchantActivityRuleConflicts', () => {
  it('warns when a new rule reuses priority and threshold in the same activity', () => {
    const conflicts = detectMerchantActivityRuleConflicts([
      {
        id: 1,
        activityId: 10,
        ruleType: 'FULL_REDUCTION',
        thresholdAmount: 100,
        discountAmount: 20,
        priority: 3,
      },
    ], {
      ...validRuleDraft,
      thresholdAmount: 100,
      discountAmount: 15,
      priority: 3,
    });

    expect(conflicts).toEqual(expect.arrayContaining([
      '已有相同优先级的活动规则，可能导致用户结算时命中顺序不清晰',
      '已有相同门槛的满减规则，请调整门槛或合并规则',
    ]));
  });

  it('warns when category discount or buy-gift scopes duplicate existing rules', () => {
    const conflicts = detectMerchantActivityRuleConflicts([
      {
        id: 2,
        activityId: 10,
        ruleType: 'CATEGORY_DISCOUNT',
        categoryCode: 'digital',
        discountRate: 0.8,
        priority: 1,
      },
      {
        id: 3,
        activityId: 10,
        ruleType: 'BUY_X_GET_Y',
        productId: 9,
        ruleConfigJson: '{"buyX":2,"getY":1}',
        priority: 2,
      },
    ], {
      ...validRuleDraft,
      ruleType: 'CATEGORY_DISCOUNT',
      thresholdAmount: '',
      discountAmount: '',
      discountRate: 0.7,
      categoryCode: ' DIGITAL ',
      priority: 4,
    });

    expect(conflicts).toContain('该分类已有折扣规则，请避免同一分类重复配置');

    expect(detectMerchantActivityRuleConflicts([
      {
        id: 3,
        activityId: 10,
        ruleType: 'BUY_X_GET_Y',
        productId: 9,
        ruleConfigJson: '{"buyX":2,"getY":1}',
        priority: 2,
      },
    ], {
      ...validRuleDraft,
      ruleType: 'BUY_X_GET_Y',
      thresholdAmount: '',
      discountAmount: '',
      productId: 9,
      priority: 5,
    })).toContain('该商品已有买赠规则，请避免重复发放权益');
  });
});
