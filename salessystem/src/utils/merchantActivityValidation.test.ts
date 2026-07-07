import { describe, expect, it } from 'vitest';
import {
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
