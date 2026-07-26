import { request } from '../request';
import type { PageResult } from '../../types/api';
import type {
  MerchantOrder,
  MerchantOrderDetail,
  MerchantOrderFilters,
} from '../../types/merchant';
import { normalizeSalesOrderDetail } from '../../utils/orderLifecycle';

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
        fulfillmentStatus: filters.fulfillmentStatus || undefined,
        deliveryStatus: filters.deliveryStatus || undefined,
        keyword: filters.keyword || undefined,
      },
      authRole: 'merchant',
    });
  },

  async getOrderDetail(tenantId: number, orderNo: string) {
    const result = await request<MerchantOrderDetail>({
      url: `/v1/merchant/tenants/${tenantId}/orders/${orderNo}`,
      method: 'get',
      authRole: 'merchant',
    });
    return normalizeSalesOrderDetail(result);
  },

  verifyPickup(tenantId: number, storeId: number, pickupCode: string) {
    return request<unknown>({
      url: `/v1/merchant/tenants/${tenantId}/orders/pickups/verify`,
      method: 'post',
      data: { storeId, pickupCode },
      authRole: 'merchant',
    });
  },

  startPreparation(tenantId: number, orderNo: string, remark?: string) {
    return request<void>({
      url: `/v1/merchant/tenants/${tenantId}/orders/${orderNo}/fulfillment/start`,
      method: 'post',
      data: { remark },
      authRole: 'merchant',
    });
  },

  completePreparation(tenantId: number, orderNo: string, remark?: string) {
    return request<void>({
      url: `/v1/merchant/tenants/${tenantId}/orders/${orderNo}/fulfillment/complete`,
      method: 'post',
      data: { remark },
      authRole: 'merchant',
    });
  },
};
