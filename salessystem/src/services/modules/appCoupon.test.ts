import { beforeEach, describe, expect, it, vi } from 'vitest';

const { mockRequest } = vi.hoisted(() => ({
  mockRequest: vi.fn(),
}));

vi.mock('../request', () => ({
  request: mockRequest,
}));

import { appCouponService } from './appCoupon';

describe('appCouponService', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('maps frontend usable status to backend received status', async () => {
    mockRequest.mockResolvedValue([]);

    await appCouponService.getMyCoupons(9, 'USABLE');

    expect(mockRequest).toHaveBeenCalledWith({
      url: '/v1/app/tenants/9/coupons',
      method: 'get',
      params: { status: 'RECEIVED' },
      authRole: 'user',
    });
  });

  it('normalizes backend user coupon fields and keeps used order trace', async () => {
    mockRequest.mockResolvedValue([
      {
        id: 501,
        couponNo: 'CP202607060001',
        templateId: 201,
        tenantId: 9,
        couponStatus: 'USED',
        templateName: '满减券',
        couponType: 'FULL_REDUCTION',
        thresholdAmount: 100,
        discountAmount: 20,
        discountRate: null,
        maxDiscountAmount: null,
        receiveTime: '2026-07-01T09:00:00',
        expireTime: '2026-07-31T23:59:59',
        useTime: '2026-07-06T10:00:00',
        orderNo: 'SO202607060001',
        trace: {
          source: '使用订单 SO202607060001',
          actionPath: '/order/SO202607060001',
          actionLabel: '查看订单',
          tone: 'neutral',
        },
      },
    ]);

    const result = await appCouponService.getMyCoupons(9, 'USED');

    expect(result).toEqual([
      {
        id: 501,
        couponNo: 'CP202607060001',
        couponTemplateId: 201,
        tenantId: 9,
        status: 'USED',
        name: '满减券',
        couponType: 'FIXED',
        thresholdAmount: 100,
        discountAmount: 20,
        discountRate: null,
        maxDiscountAmount: null,
        receiveTime: '2026-07-01T09:00:00',
        expireTime: '2026-07-31T23:59:59',
        usedTime: '2026-07-06T10:00:00',
        orderNo: 'SO202607060001',
        trace: {
          source: '使用订单 SO202607060001',
          actionPath: '/order/SO202607060001',
          actionLabel: '查看订单',
          tone: 'neutral',
        },
        timeline: [],
      },
    ]);
  });

  it('keeps locked coupons distinguishable for the active coupon list', async () => {
    mockRequest.mockResolvedValue([
      {
        id: 502,
        couponNo: 'CP202607060002',
        templateId: 202,
        tenantId: 9,
        couponStatus: 'LOCKED',
        templateName: '锁定券',
        couponType: 'FULL_REDUCTION',
        thresholdAmount: 50,
        discountAmount: 10,
        receiveTime: '2026-07-01T09:00:00',
        expireTime: '2026-07-31T23:59:59',
        orderNo: 'SO202607060002',
      },
    ]);

    const result = await appCouponService.getMyCoupons(9, 'LOCKED');

    expect(mockRequest).toHaveBeenCalledWith({
      url: '/v1/app/tenants/9/coupons',
      method: 'get',
      params: { status: 'LOCKED' },
      authRole: 'user',
    });
    expect(result[0].status).toBe('LOCKED');
    expect(result[0].orderNo).toBe('SO202607060002');
  });
});
