import { beforeEach, describe, expect, it, vi } from 'vitest';

const { mockRequest } = vi.hoisted(() => ({
  mockRequest: vi.fn(),
}));

vi.mock('../request', () => ({
  request: mockRequest,
}));

import { adminMarketingService } from './adminMarketing';

describe('adminMarketingService', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('maps platform activity creation payload to backend DTO names', async () => {
    mockRequest.mockResolvedValue({
      id: 12,
      tenantId: null,
      activityScope: 'PLATFORM',
      activityName: '平台满减',
      activityType: 'FULL_REDUCTION',
      startTime: '2026-08-01T10:00:00',
      endTime: '2026-08-10T10:00:00',
      status: 'DRAFT',
      description: '平台活动',
    });

    const result = await adminMarketingService.createActivity({
      name: '平台满减',
      activityType: 'FULL_REDUCTION',
      startTime: '2026-08-01T10:00:00.000Z',
      endTime: '2026-08-10T10:00:00.000Z',
      description: '平台活动',
    });

    expect(mockRequest).toHaveBeenCalledWith({
      url: '/v1/admin/marketing/activities',
      method: 'post',
      data: {
        activityName: '平台满减',
        activityType: 'FULL_REDUCTION',
        startTime: '2026-08-01T10:00:00.000Z',
        endTime: '2026-08-10T10:00:00.000Z',
        description: '平台活动',
      },
      authRole: 'admin',
    });
    expect(result).toMatchObject({
      id: 12,
      ownerType: 'PLATFORM',
      name: '平台满减',
    });
  });

  it('normalizes backend platform activity and legacy discount rule fields', async () => {
    mockRequest
      .mockResolvedValueOnce([
        {
          id: 13,
          tenantId: null,
          activityScope: 'PLATFORM',
          activityName: '平台折扣',
          activityType: 'FULL_DISCOUNT',
          startTime: '2026-09-01T10:00:00',
          endTime: '2026-09-10T10:00:00',
          status: 'ACTIVE',
        },
      ])
      .mockResolvedValueOnce({
        id: 21,
        activityId: 13,
        ruleType: 'FULL_DISCOUNT',
        thresholdAmount: 100,
        discountRate: 0.85,
        priority: 0,
      });

    const activities = await adminMarketingService.getActivities('ACTIVE');
    const rule = await adminMarketingService.addActivityRule(13, {
      ruleType: 'FULL_DISCOUNT',
      thresholdAmount: 100,
      discountRate: 0.85,
      priority: 0,
    });

    expect(activities[0]).toMatchObject({
      id: 13,
      ownerType: 'PLATFORM',
      name: '平台折扣',
      activityType: 'DISCOUNT_RATE',
    });
    expect(mockRequest).toHaveBeenLastCalledWith({
      url: '/v1/admin/marketing/activities/13/rules',
      method: 'post',
      data: {
        ruleType: 'DISCOUNT_RATE',
        thresholdAmount: 100,
        discountRate: 0.85,
        priority: 0,
      },
      authRole: 'admin',
    });
    expect(rule.ruleType).toBe('DISCOUNT_RATE');
  });
});
