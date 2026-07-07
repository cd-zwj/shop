import { beforeEach, describe, expect, it, vi } from 'vitest';

const mockRequest = vi.fn();
vi.mock('../request', () => ({
  request: (...args: unknown[]) => mockRequest(...args),
}));

import { merchantAnalyticsService } from './merchantAnalytics';

beforeEach(() => {
  vi.clearAllMocks();
});

describe('merchantAnalyticsService', () => {
  it('loads product sales rank with merchant auth and filters', async () => {
    const rank = [{
      productId: 1,
      productCode: '1',
      productName: '测试商品',
      productImage: null,
      salesQuantity: 3,
      salesAmount: 129,
    }];
    mockRequest.mockResolvedValue(rank);

    const result = await merchantAnalyticsService.getProductSalesRank(9, {
      startDate: '2026-07-01',
      endDate: '2026-07-31',
      limit: 5,
    });

    expect(mockRequest).toHaveBeenCalledWith({
      url: '/v1/merchant/tenants/9/analytics/product-rank',
      method: 'get',
      params: {
        startDate: '2026-07-01',
        endDate: '2026-07-31',
        limit: 5,
      },
      authRole: 'merchant',
    });
    expect(result).toEqual(rank);
  });
});
