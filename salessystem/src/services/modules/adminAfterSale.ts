import { request } from '../request';
import type { PageResult } from '../../types/api';
import type { AdminAfterSale, AfterSaleAction } from '../../types/refund';

export interface AdminAfterSaleListParams {
  tenantId?: number;
  status?: string;
  keyword?: string;
  pageNum?: number;
  pageSize?: number;
}

export const adminAfterSaleService = {
  listRefunds(params: AdminAfterSaleListParams = {}) {
    return request<PageResult<AdminAfterSale>>({
      url: '/v1/admin/refunds',
      method: 'get',
      params: {
        tenantId: params.tenantId,
        status: params.status || undefined,
        keyword: params.keyword || undefined,
        pageNum: params.pageNum ?? 1,
        pageSize: params.pageSize ?? 20,
      },
      authRole: 'admin',
    });
  },

  getRefund(tenantId: number, refundId: number) {
    return request<AdminAfterSale>({
      url: `/v1/admin/tenants/${tenantId}/refunds/${refundId}`,
      method: 'get',
      authRole: 'admin',
    });
  },

  listActions(tenantId: number, refundId: number) {
    return request<AfterSaleAction[]>({
      url: `/v1/admin/tenants/${tenantId}/refunds/${refundId}/actions`,
      method: 'get',
      authRole: 'admin',
    });
  },

  intervene(
    tenantId: number,
    refundId: number,
    expectedStatus: string,
    approved: boolean,
    remark: string,
  ) {
    return request<void>({
      url: `/v1/admin/tenants/${tenantId}/refunds/${refundId}/intervene`,
      method: 'put',
      data: { expectedStatus, approved, remark },
      authRole: 'admin',
    });
  },
};
