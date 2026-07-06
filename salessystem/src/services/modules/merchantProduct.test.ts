import { beforeEach, describe, expect, it, vi } from 'vitest';

const mockRequest = vi.fn();
vi.mock('../request', () => ({
  request: (...args: unknown[]) => mockRequest(...args),
}));

import { merchantProductService } from './merchantProduct';

beforeEach(() => {
  vi.clearAllMocks();
});

describe('merchantProductService', () => {
  it('loads product change logs with merchant auth', async () => {
    const page = {
      records: [
        {
          id: 1,
          productId: 10,
          changeType: 'PRICE',
          fieldName: 'price',
          oldValue: '28.00',
          newValue: '32.00',
        },
      ],
      total: 1,
      page: 1,
      current: 1,
      size: 5,
      pages: 1,
    };
    mockRequest.mockResolvedValue(page);

    const result = await merchantProductService.listChangeLogs(2, 10, { current: 1, size: 5 });

    expect(mockRequest).toHaveBeenCalledWith({
      url: '/v1/merchant/tenants/2/products/10/change-logs',
      method: 'get',
      params: {
        current: 1,
        size: 5,
      },
      authRole: 'merchant',
    });
    expect(result).toEqual(page);
  });
});
