import { beforeEach, describe, expect, it, vi } from 'vitest';

const { mockRequest } = vi.hoisted(() => ({ mockRequest: vi.fn() }));

vi.mock('../request', () => ({ request: mockRequest }));

import { adminAfterSaleService } from './adminAfterSale';

describe('adminAfterSaleService', () => {
  beforeEach(() => vi.clearAllMocks());

  it('uses the first page and omits empty optional filters by default', () => {
    adminAfterSaleService.listRefunds();

    expect(mockRequest).toHaveBeenCalledWith({
      url: '/v1/admin/refunds',
      method: 'get',
      params: {
        tenantId: undefined,
        status: undefined,
        keyword: undefined,
        pageNum: 1,
        pageSize: 20,
      },
      authRole: 'admin',
    });
  });

  it('lists cross-tenant refunds with normalized optional filters', () => {
    adminAfterSaleService.listRefunds({
      tenantId: 9, status: 'PENDING', keyword: 'RA-1', pageNum: 2, pageSize: 20,
    });

    expect(mockRequest).toHaveBeenCalledWith({
      url: '/v1/admin/refunds',
      method: 'get',
      params: { tenantId: 9, status: 'PENDING', keyword: 'RA-1', pageNum: 2, pageSize: 20 },
      authRole: 'admin',
    });
  });

  it('uses tenant-bound URLs for detail and immutable actions', () => {
    adminAfterSaleService.getRefund(9, 12);
    adminAfterSaleService.listActions(9, 12);

    expect(mockRequest).toHaveBeenNthCalledWith(1, {
      url: '/v1/admin/tenants/9/refunds/12', method: 'get', authRole: 'admin',
    });
    expect(mockRequest).toHaveBeenNthCalledWith(2, {
      url: '/v1/admin/tenants/9/refunds/12/actions', method: 'get', authRole: 'admin',
    });
  });

  it('sends expected status with an intervention decision', () => {
    adminAfterSaleService.intervene(9, 12, 'PENDING', false, '凭证不完整');

    expect(mockRequest).toHaveBeenCalledWith({
      url: '/v1/admin/tenants/9/refunds/12/intervene',
      method: 'put',
      data: { expectedStatus: 'PENDING', approved: false, remark: '凭证不完整' },
      authRole: 'admin',
    });
  });
});
