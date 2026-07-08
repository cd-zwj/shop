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

  it('loads merchant visible system tasks with merchant auth', async () => {
    const page = {
      records: [{
        taskSource: 'compensation',
        id: 11,
        taskNo: 'CT202607080001',
        taskType: 'MERCHANT_APPROVED_REFUND',
        bizType: 'MERCHANT_APPROVED_REFUND',
        bizNo: 'RA202607080001',
        taskStatus: 'FAIL',
        retryCount: 5,
        maxRetryCount: null,
        nextRetryTime: null,
        lastError: 'Provider refund is not supported in phase 1',
        createTime: '2026-07-08T10:00:00',
        updateTime: '2026-07-08T10:05:00',
        actionLabel: '查看退款单',
        actionPath: '/merchant/refunds?status=FAILED',
      }],
      total: 1,
      page: 1,
      current: 1,
      size: 20,
      pages: 1,
    };
    mockRequest.mockResolvedValue(page);

    const result = await merchantWorkbenchService.listTasks(9, { type: 'compensation', pageNum: 1, pageSize: 20 });

    expect(mockRequest).toHaveBeenCalledWith({
      url: '/v1/merchant/tenants/9/workbench/tasks',
      method: 'get',
      params: { type: 'compensation', pageNum: 1, pageSize: 20 },
      authRole: 'merchant',
    });
    expect(result).toEqual(page);
  });
});
