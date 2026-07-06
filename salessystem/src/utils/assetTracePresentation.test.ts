import { describe, expect, it } from 'vitest';
import {
  getCouponTracePresentation,
  getGrowthTracePresentation,
  getPointsTracePresentation,
} from './assetTracePresentation';
import type { UserCoupon } from '../types/coupon';
import type { GrowthLog } from '../types/growth';
import type { PointsLog } from '../types/points';

describe('assetTracePresentation', () => {
  it('explains earned points with order source and balance trace', () => {
    const log: PointsLog = {
      id: 1,
      tenantId: 10,
      userId: 20,
      points: 120,
      balance: 520,
      type: 'GRANT',
      reason: '订单支付返积分',
      expireTime: '2026-12-31T23:59:59',
      orderNo: 'SO20260706001',
      createTime: '2026-07-06T10:30:00',
    };

    expect(getPointsTracePresentation(log)).toEqual({
      title: '订单支付返积分',
      source: '来源：订单 SO20260706001',
      effect: '+120 积分',
      balance: '变动后余额 520',
      hint: '将于 2026-12-31 23:59 过期',
      actionPath: '/order/SO20260706001',
      actionLabel: '查看订单',
      tone: 'positive',
    });
  });

  it('explains deducted points without assuming an order link', () => {
    const log: PointsLog = {
      id: 2,
      tenantId: 10,
      userId: 20,
      points: -80,
      balance: 440,
      type: 'DEDUCT',
      reason: '兑换商品',
      orderNo: null,
      createTime: '2026-07-06T11:00:00',
    };

    const presentation = getPointsTracePresentation(log);

    expect(presentation.source).toBe('来源：兑换商品');
    expect(presentation.effect).toBe('-80 积分');
    expect(presentation.actionPath).toBeUndefined();
    expect(presentation.tone).toBe('negative');
  });

  it('explains growth changes with before and after values', () => {
    const log: GrowthLog = {
      id: 3,
      changeType: 'EARN',
      changeGrowth: 35,
      growthBefore: 100,
      growthAfter: 135,
      bizType: 'ORDER',
      bizNo: 'SO20260706002',
      remark: null,
      createTime: '2026-07-06T12:00:00',
    };

    expect(getGrowthTracePresentation(log)).toEqual({
      title: '订单消费',
      source: '来源：订单 SO20260706002',
      effect: '+35 成长值',
      balance: '100 -> 135',
      actionPath: '/order/SO20260706002',
      actionLabel: '查看订单',
      tone: 'positive',
    });
  });

  it('explains usable coupon origin and suggested action', () => {
    const coupon: UserCoupon = {
      id: 4,
      couponNo: 'CP20260706001',
      couponTemplateId: 40,
      tenantId: 10,
      status: 'USABLE',
      name: '夏季满减券',
      couponType: 'FIXED',
      thresholdAmount: 100,
      discountAmount: 20,
      discountRate: null,
      maxDiscountAmount: null,
      receiveTime: '2026-07-06T09:00:00',
      expireTime: '2026-07-20T23:59:59',
      usedTime: null,
    };

    expect(getCouponTracePresentation(coupon)).toEqual({
      source: '领取时间 2026-07-06 09:00',
      status: '可使用',
      hint: '满 100 元可用，有效期至 2026-07-20 23:59',
      actionLabel: '去使用',
      actionPath: '/?tenantId=10',
      tone: 'positive',
    });
  });

  it('explains used coupon lifecycle', () => {
    const coupon: UserCoupon = {
      id: 5,
      couponNo: 'CP20260706002',
      couponTemplateId: 41,
      tenantId: 10,
      status: 'USED',
      name: '折扣券',
      couponType: 'RATE',
      thresholdAmount: 0,
      discountAmount: null,
      discountRate: 0.8,
      maxDiscountAmount: 30,
      receiveTime: '2026-07-01T09:00:00',
      expireTime: '2026-07-31T23:59:59',
      usedTime: '2026-07-06T13:10:00',
    };

    const presentation = getCouponTracePresentation(coupon);

    expect(presentation.status).toBe('已使用');
    expect(presentation.hint).toBe('使用时间 2026-07-06 13:10');
    expect(presentation.actionPath).toBeUndefined();
    expect(presentation.tone).toBe('neutral');
  });
});
