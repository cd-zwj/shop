import { describe, expect, it } from 'vitest';
import type { CouponTemplate, UserCoupon } from '../types/coupon';
import {
  calculateCartCouponDiscount,
  getSelectableCartCoupons,
  resolveSelectedCartCoupon,
} from './cartCoupons';

const ownedCoupon: UserCoupon = {
  id: 10,
  couponNo: 'UC10',
  couponTemplateId: 1,
  tenantId: 3,
  status: 'USABLE',
  name: '满百减二十',
  couponType: 'FIXED',
  thresholdAmount: 100,
  discountAmount: 20,
  discountRate: null,
  maxDiscountAmount: null,
  receiveTime: '2026-07-01T00:00:00',
  expireTime: '2026-08-01T00:00:00',
  usedTime: null,
};

const templateCoupon: CouponTemplate = {
  id: 2,
  tenantId: 3,
  ownerType: 'TENANT',
  name: '九折券',
  couponType: 'RATE',
  thresholdAmount: 50,
  discountAmount: null,
  discountRate: 0.9,
  maxDiscountAmount: 15,
  perUserLimit: 1,
  remainingStock: 5,
  receivedByCurrentUser: 0,
  receivable: true,
  receiveStartTime: '2026-07-01T00:00:00',
  receiveEndTime: '2026-08-01T00:00:00',
  validStartTime: null,
  validEndTime: null,
  validDaysAfterReceive: 7,
  description: null,
};

describe('cartCoupons', () => {
  // 小计单位为分；券面额/门槛单位为元（详见 cartCoupons.ts 顶部单位约定）。
  it('calculates fixed and capped rate discounts in fen', () => {
    // 满 ¥100 减 ¥20：小计 ¥120（12000 分）→ 折扣 2000 分
    expect(calculateCartCouponDiscount(ownedCoupon, 12000)).toBe(2000);
    // 九折封顶 ¥15：小计 ¥300（30000 分）→ 10% 折扣 3000 分超上限，取 1500 分
    expect(calculateCartCouponDiscount(templateCoupon, 30000)).toBe(1500);
  });

  it('marks selected coupons unusable when refreshed subtotal no longer reaches threshold', () => {
    const resolution = resolveSelectedCartCoupon(
      { availableTemplates: [], myUsableCoupons: [ownedCoupon] },
      { key: 'owned-10', id: 10, type: 'OWNED', name: '满百减二十' },
      8000,
    );

    expect(resolution).toEqual({
      discountAmount: 0,
      isUsable: false,
      reason: '还差 ¥20.00',
    });
  });

  it('explains template coupon receive constraints before checkout', () => {
    const [option] = getSelectableCartCoupons(
      {
        availableTemplates: [{ ...templateCoupon, receivable: false }],
        myUsableCoupons: [],
      },
      10000,
    );

    expect(option).toMatchObject({
      key: 'template-2',
      isUsable: false,
      reason: '已领超限',
    });
  });
});
