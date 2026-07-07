import { describe, expect, it } from 'vitest';
import { validateMerchantCouponDraft } from './merchantCouponValidation';

const validDraft = {
  name: '夏季满减券',
  couponType: 'FIXED' as const,
  thresholdAmount: 100,
  discountAmount: 20,
  discountRate: '',
  maxDiscountAmount: '',
  totalStock: 100,
  perUserLimit: 1,
  validityType: 'DAYS' as const,
  validDaysAfterReceive: 30,
  validStartTime: '',
  validEndTime: '',
  receiveStartTime: '',
  receiveEndTime: '',
};

describe('validateMerchantCouponDraft', () => {
  it('accepts a valid relative-validity fixed coupon draft', () => {
    expect(validateMerchantCouponDraft(validDraft)).toEqual([]);
  });

  it('blocks per-user limit greater than total stock', () => {
    expect(validateMerchantCouponDraft({
      ...validDraft,
      totalStock: 3,
      perUserLimit: 5,
    })).toContain('每人限领不能大于发行总量');
  });

  it('blocks invalid fixed validity windows and receive windows', () => {
    expect(validateMerchantCouponDraft({
      ...validDraft,
      validityType: 'RANGE',
      validStartTime: '2026-08-02T10:00',
      validEndTime: '2026-08-01T10:00',
      receiveStartTime: '2026-08-03T10:00',
      receiveEndTime: '2026-08-01T10:00',
    })).toEqual(expect.arrayContaining([
      '有效期结束时间必须晚于开始时间',
      '领取结束时间必须晚于领取开始时间',
    ]));
  });

  it('blocks receive windows outside a fixed validity window', () => {
    expect(validateMerchantCouponDraft({
      ...validDraft,
      validityType: 'RANGE',
      validStartTime: '2026-08-02T10:00',
      validEndTime: '2026-08-10T10:00',
      receiveStartTime: '2026-08-01T10:00',
      receiveEndTime: '2026-08-11T10:00',
    })).toEqual(expect.arrayContaining([
      '领取开始时间不能早于优惠券有效期开始时间',
      '领取结束时间不能晚于优惠券有效期结束时间',
    ]));
  });

  it('blocks discount values that make a coupon self-conflicting', () => {
    expect(validateMerchantCouponDraft({
      ...validDraft,
      discountAmount: 120,
    })).toContain('满减面值不能大于门槛金额');

    expect(validateMerchantCouponDraft({
      ...validDraft,
      couponType: 'RATE',
      discountAmount: '',
      discountRate: 1,
      maxDiscountAmount: -1,
    })).toEqual(expect.arrayContaining([
      '折扣比例必须大于 0 且小于 1',
      '折扣封顶金额不能小于 0',
    ]));
  });
});
