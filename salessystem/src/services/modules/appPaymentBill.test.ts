import { describe, expect, it, vi } from 'vitest';

const { mockRequest } = vi.hoisted(() => ({
  mockRequest: vi.fn(),
}));

vi.mock('../request', () => ({
  request: mockRequest,
}));

import { appPaymentBillService } from './appPaymentBill';

describe('appPaymentBillService', () => {
  it('queries latest payment bill by biz type and biz number for notification and wallet links', async () => {
    mockRequest.mockResolvedValueOnce({
      billNo: 'PB202607060001',
      bizType: 'RECHARGE',
      bizNo: 'WR202607060001',
      payStatus: 'SUCCESS',
    });

    const result = await appPaymentBillService.getLatestPaymentBillByBiz('RECHARGE', 'WR202607060001');

    expect(result.billNo).toBe('PB202607060001');
    expect(mockRequest).toHaveBeenCalledWith({
      url: '/v1/app/payment-bills/latest',
      method: 'get',
      params: { bizType: 'RECHARGE', bizNo: 'WR202607060001' },
      authRole: 'user',
    });
  });
});
