import { request } from '../request';
import type { AfterSaleAction, Refund, RefundCreateDTO } from '../../types/refund';
import type { PageResult } from '../../types/api';

export const appRefundService = {
  applyRefund(tenantId: number, payload: RefundCreateDTO) {
    return request<Refund>({
      url: `/v1/app/tenants/${tenantId}/refunds`,
      method: 'post',
      data: payload,
      authRole: 'user',
    });
  },

  listRefunds(tenantId: number, status?: string, pageNum = 1, pageSize = 10) {
    return request<PageResult<Refund>>({
      url: `/v1/app/tenants/${tenantId}/refunds`,
      method: 'get',
      params: { status, pageNum, pageSize },
      authRole: 'user',
    });
  },

  getRefundDetail(tenantId: number, refundId: number) {
    return request<Refund>({
      url: `/v1/app/tenants/${tenantId}/refunds/${refundId}`,
      method: 'get',
      authRole: 'user',
    });
  },

  cancelRefund(tenantId: number, refundId: number) {
    return request<void>({
      url: `/v1/app/tenants/${tenantId}/refunds/${refundId}/cancel`,
      method: 'put',
      authRole: 'user',
    });
  },

  listActions(tenantId: number, refundId: number) {
    return request<AfterSaleAction[]>({
      url: `/v1/app/tenants/${tenantId}/refunds/${refundId}/actions`,
      method: 'get',
      authRole: 'user',
    });
  },
};
