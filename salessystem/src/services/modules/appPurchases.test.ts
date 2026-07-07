import { beforeEach, describe, expect, it, vi } from 'vitest';
import { appPurchasesService } from './appPurchases';

const mockRequest = vi.fn();

vi.mock('../request', () => ({
  request: (...args: unknown[]) => mockRequest(...args),
}));

beforeEach(() => {
  vi.clearAllMocks();
});

describe('appPurchasesService', () => {
  it('passes orderNo to purchase list query for direct order fulfillment lookup', async () => {
    mockRequest.mockResolvedValue({ records: [], total: 0, page: 1, size: 50, pages: 0 });

    await appPurchasesService.list(undefined, 1, 50, 'SO202607060001');

    expect(mockRequest).toHaveBeenCalledWith({
      url: '/v1/app/purchases',
      method: 'get',
      params: {
        status: undefined,
        current: 1,
        size: 50,
        orderNo: 'SO202607060001',
      },
      authRole: 'user',
    });
  });
});
