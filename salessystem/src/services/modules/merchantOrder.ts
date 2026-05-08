import { request } from '../request';
import type { PageResult } from '../../types/api';
import type {
  MerchantOrder,
  MerchantOrderDetail,
  MerchantOrderFilters,
} from '../../types/merchant';

export const merchantOrderService = {
  listOrders(tenantId: number, filters: MerchantOrderFilters = {}) {
    return request<PageResult<MerchantOrder>>({
      url: `/v1/merchant/tenants/${tenantId}/orders`,
      method: 'get',
      params: {
        current: filters.current ?? 1,
        size: filters.size ?? 10,
        orderStatus: filters.orderStatus || undefined,
        payStatus: filters.payStatus || undefined,
        keyword: filters.keyword || undefined,
      },
      authRole: 'merchant',
    });
  },

  getOrderDetail(tenantId: number, orderNo: string) {
    return request<MerchantOrderDetail>({
      url: `/v1/merchant/tenants/${tenantId}/orders/${orderNo}`,
      method: 'get',
      authRole: 'merchant',
    });
  },
};
