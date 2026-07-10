import { beforeEach, describe, expect, it, vi } from 'vitest';

const { mockRequest } = vi.hoisted(() => ({
  mockRequest: vi.fn(),
}));

vi.mock('../request', () => ({
  request: mockRequest,
}));

import { merchantMarketingService } from './merchantMarketing';

describe('merchantMarketingService', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('maps merchant activity creation payload to backend DTO names', async () => {
    mockRequest.mockResolvedValue({
      id: 7,
      tenantId: 3,
      activityScope: 'TENANT',
      activityName: '暑期满减',
      activityType: 'FULL_REDUCTION',
      startTime: '2026-08-01T10:00:00',
      endTime: '2026-08-10T10:00:00',
      status: 'DRAFT',
      description: '本地促销',
    });

    const result = await merchantMarketingService.createActivity(3, {
      name: '暑期满减',
      activityType: 'FULL_REDUCTION',
      startTime: '2026-08-01T10:00:00.000Z',
      endTime: '2026-08-10T10:00:00.000Z',
      description: '本地促销',
    });

    expect(mockRequest).toHaveBeenCalledWith({
      url: '/v1/merchant/tenants/3/marketing/activities',
      method: 'post',
      data: {
        activityName: '暑期满减',
        activityType: 'FULL_REDUCTION',
        startTime: '2026-08-01T10:00:00.000Z',
        endTime: '2026-08-10T10:00:00.000Z',
        description: '本地促销',
      },
      authRole: 'merchant',
    });
    expect(result.name).toBe('暑期满减');
    expect(result.ownerType).toBe('TENANT');
  });

  it('maps merchant coupon creation payload to backend DTO names with member restrictions', async () => {
    mockRequest.mockResolvedValue({
      id: 9,
      templateName: '银卡专享',
      couponType: 'FULL_REDUCTION',
    });

    await merchantMarketingService.createCouponTemplate(3, {
      name: '银卡专享',
      couponType: 'FIXED',
      thresholdAmount: 100,
      discountAmount: 10,
      totalStock: 50,
      perUserLimit: 1,
      validDaysAfterReceive: 30,
      requiredMemberLevel: 2,
      requiredMemberTagIds: '5,6',
      excludedMemberTagIds: '9',
    });

    expect(mockRequest).toHaveBeenCalledWith({
      url: '/v1/merchant/tenants/3/marketing/coupons',
      method: 'post',
      data: {
        templateName: '银卡专享',
        couponType: 'FULL_REDUCTION',
        thresholdAmount: 100,
        discountAmount: 10,
        discountRate: undefined,
        maxDiscountAmount: undefined,
        totalQuantity: 50,
        perUserLimit: 1,
        receiveStartTime: undefined,
        receiveEndTime: undefined,
        validStartTime: undefined,
        validEndTime: undefined,
        validDays: 30,
        description: undefined,
        requiredMemberLevel: 2,
        requiredMemberTagIds: '5,6',
        excludedMemberTagIds: '9',
      },
      authRole: 'merchant',
    });
  });

  it('normalizes backend activity and rule fields for merchant pages', async () => {
    mockRequest.mockResolvedValue([
      {
        id: 8,
        tenantId: 3,
        activityScope: 'TENANT',
        activityName: '折扣活动',
        activityType: 'DISCOUNT_RATE',
        startTime: '2026-09-01T10:00:00',
        endTime: '2026-09-10T10:00:00',
        status: 'ACTIVE',
      },
    ]);

    const activities = await merchantMarketingService.getActivities(3, 'ACTIVE');

    expect(activities[0]).toMatchObject({
      id: 8,
      ownerType: 'TENANT',
      name: '折扣活动',
      activityType: 'DISCOUNT_RATE',
    });
  });
});
