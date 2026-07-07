import { beforeEach, describe, expect, it, vi } from 'vitest';

const mockRequest = vi.fn();
vi.mock('../request', () => ({
  request: (...args: unknown[]) => mockRequest(...args),
}));

import { merchantWorkbenchService } from './merchantWorkbench';

beforeEach(() => {
  vi.clearAllMocks();
});

describe('merchantWorkbenchService', () => {
  it('loads merchant todo summary with merchant auth', async () => {
    const summary = {
      totalCount: 3,
      items: [
        {
          key: 'fulfillment',
          label: '待履约订单',
          description: '需要商家处理',
          count: 3,
          path: '/merchant/orders?tab=shipping',
          tone: 'blue',
        },
      ],
    };
    mockRequest.mockResolvedValue(summary);

    const result = await merchantWorkbenchService.getTodoSummary(9);

    expect(mockRequest).toHaveBeenCalledWith({
      url: '/v1/merchant/tenants/9/workbench/todos',
      method: 'get',
      authRole: 'merchant',
    });
    expect(result).toEqual(summary);
  });
});
