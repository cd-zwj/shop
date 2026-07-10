import { describe, expect, it } from 'vitest';
import {
  detectMerchantCrossActivityRuleConflicts,
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

describe('detectMerchantCrossActivityRuleConflicts', () => {
  const activities = [
    {
      id: 10,
      tenantId: 1,
      ownerType: 'TENANT' as const,
      name: '活动A',
      activityType: 'FULL_REDUCTION',
      startTime: '2026-08-01T10:00',
      endTime: '2026-08-10T10:00',
      status: 'ACTIVE',
    },
    {
      id: 11,
      tenantId: 1,
      ownerType: 'TENANT' as const,
      name: '活动B',
      activityType: 'FULL_REDUCTION',
      startTime: '2026-08-05T10:00',
      endTime: '2026-08-12T10:00',
      status: 'DRAFT',
    },
    {
      id: 12,
      tenantId: 1,
      ownerType: 'TENANT' as const,
      name: '活动C',
      activityType: 'FULL_REDUCTION',
      startTime: '2026-08-20T10:00',
      endTime: '2026-08-25T10:00',
      status: 'ACTIVE',
    },
    {
      id: 13,
      tenantId: 1,
      ownerType: 'TENANT' as const,
      name: '活动D',
      activityType: 'FULL_REDUCTION',
      startTime: '2026-08-05T10:00',
      endTime: '2026-08-12T10:00',
      status: 'DISABLED',
    },
  ];

  it('detects duplicate priority and threshold only from overlapping active/draft activities', () => {
    const conflicts = detectMerchantCrossActivityRuleConflicts(
      activities,
      {
        11: [{ id: 1, activityId: 11, ruleType: 'FULL_REDUCTION', thresholdAmount: 100, discountAmount: 10, priority: 3 }],
        12: [{ id: 2, activityId: 12, ruleType: 'FULL_REDUCTION', thresholdAmount: 100, discountAmount: 5, priority: 9 }],
        13: [{ id: 3, activityId: 13, ruleType: 'FULL_REDUCTION', thresholdAmount: 100, discountAmount: 8, priority: 3 }],
      },
      activities[0],
      {
        ...validRuleDraft,
        thresholdAmount: 100,
        priority: 3,
      },
    );

    expect(conflicts).toEqual(expect.arrayContaining([
      '存在其他活动使用相同优先级的规则，请避免跨活动命中顺序不清晰',
      '存在其他活动配置了相同门槛的满减规则',
    ]));
  });

  it('ignores non-overlapping and disabled activities', () => {
    const conflicts = detectMerchantCrossActivityRuleConflicts(
      activities,
      {
        12: [{ id: 2, activityId: 12, ruleType: 'FULL_REDUCTION', thresholdAmount: 100, discountAmount: 5, priority: 3 }],
        13: [{ id: 3, activityId: 13, ruleType: 'FULL_REDUCTION', thresholdAmount: 100, discountAmount: 8, priority: 3 }],
      },
      activities[0],
      {
        ...validRuleDraft,
        thresholdAmount: 100,
        priority: 3,
      },
    );

    expect(conflicts).toEqual([]);
  });

  it('normalizes FULL_DISCOUNT to DISCOUNT_RATE for threshold conflict detection', () => {
    const conflicts = detectMerchantCrossActivityRuleConflicts(
      activities,
      {
        11: [{ id: 4, activityId: 11, ruleType: 'DISCOUNT_RATE', thresholdAmount: 200, discountRate: 0.8, priority: 1 }],
      },
      {
        id: activities[0].id,
        startTime: activities[0].startTime,
        endTime: activities[0].endTime,
        status: activities[0].status,
      },
      {
        ...validRuleDraft,
        ruleType: 'FULL_DISCOUNT',
        thresholdAmount: 200,
        discountAmount: '',
        discountRate: 0.7,
        priority: 2,
      },
    );

    expect(conflicts).toContain('存在其他活动配置了相同门槛的满折规则');
  });

  it('detects duplicate buy-x-get-y product rules across overlapping activities', () => {
    const conflicts = detectMerchantCrossActivityRuleConflicts(
      activities,
      {
        11: [{ id: 5, activityId: 11, ruleType: 'BUY_X_GET_Y', productId: 88, ruleConfigJson: '{"giftProductId":99}', priority: 0 }],
      },
      {
        id: activities[0].id,
        startTime: activities[0].startTime,
        endTime: activities[0].endTime,
        status: activities[0].status,
      },
      {
        ...validRuleDraft,
        ruleType: 'BUY_X_GET_Y',
        thresholdAmount: '',
        discountAmount: '',
        discountRate: '',
        productId: 88,
        ruleConfigJson: '{"giftProductId":100}',
        priority: 1,
      },
    );

    expect(conflicts).toContain('存在其他活动使用同一商品配置买赠规则');
  });

  it('detects duplicate category discount rules across overlapping activities', () => {
    const conflicts = detectMerchantCrossActivityRuleConflicts(
      activities,
      {
        11: [{ id: 6, activityId: 11, ruleType: 'CATEGORY_DISCOUNT', categoryCode: 'FOOD', discountRate: 0.9, priority: 1 }],
      },
      {
        id: activities[0].id,
        startTime: activities[0].startTime,
        endTime: activities[0].endTime,
        status: activities[0].status,
      },
      {
        ...validRuleDraft,
        ruleType: 'CATEGORY_DISCOUNT',
        thresholdAmount: '',
        discountAmount: '',
        discountRate: 0.8,
        categoryCode: 'FOOD',
        priority: 2,
      },
    );

    expect(conflicts).toContain('存在其他活动使用同一分类配置折扣规则');
  });
});
