import { beforeEach, describe, expect, it, vi } from 'vitest';

const { mockRequest } = vi.hoisted(() => ({
  mockRequest: vi.fn(),
}));

vi.mock('../request', () => ({
  request: mockRequest,
}));

import { appPointsService } from './appPoints';

describe('appPointsService', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('normalizes backend points logs and keeps backend trace presentation', async () => {
    mockRequest.mockResolvedValue({
      records: [
        {
          id: 301,
          bizType: 'ORDER_REWARD',
          bizNo: 'SO202607070001',
          changePoints: 120,
          pointsAfter: 320,
          remark: '订单返积分',
          expireTime: '2026-08-07T23:59:00',
          createTime: '2026-07-07T10:00:00',
          trace: {
            title: '订单返积分',
            source: '来源：订单返积分 SO202607070001',
            actionLabel: '查看订单',
            actionPath: '/order/SO202607070001',
            tone: 'positive',
          },
        },
      ],
      total: 1,
      page: 1,
      size: 20,
      pages: 1,
    });

    const result = await appPointsService.getPointsLogs(9, 1, 20);

    expect(mockRequest).toHaveBeenCalledWith({
      url: '/v1/app/tenants/9/points/logs',
      method: 'get',
      params: { current: 1, size: 20 },
      authRole: 'user',
    });
    expect(result.records[0]).toEqual({
      id: 301,
      tenantId: 9,
      userId: 0,
      points: 120,
      balance: 320,
      type: 'GRANT',
      reason: '订单返积分',
      expireTime: '2026-08-07T23:59:00',
      orderNo: 'SO202607070001',
      createTime: '2026-07-07T10:00:00',
      trace: {
        title: '订单返积分',
        source: '来源：订单返积分 SO202607070001',
        actionLabel: '查看订单',
        actionPath: '/order/SO202607070001',
        tone: 'positive',
      },
    });
  });
});
