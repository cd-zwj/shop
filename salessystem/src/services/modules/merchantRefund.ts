import { request } from '../request';
import type { Refund } from '../../types/refund';
import type { PageResult } from '../../types/api';

export const merchantRefundService = {
  listRefunds(tenantId: number, status?: string, pageNum = 1, pageSize = 10) {
    return request<PageResult<Refund>>({
      url: `/v1/merchant/tenants/${tenantId}/refunds`,
      method: 'get',
      params: { status, pageNum, pageSize },
      authRole: 'merchant',
    });
  },

  auditRefund(tenantId: number, refundId: number, approved: boolean, rejectReason?: string) {
    return request<void>({
      url: `/v1/merchant/tenants/${tenantId}/refunds/${refundId}/audit`,
      method: 'put',
      data: { approved, rejectReason },
      authRole: 'merchant',
    });
  },
};
