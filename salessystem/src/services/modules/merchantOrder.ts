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

  /**
   * 实物商品发货 - 提交物流单号后将订单项的 deliveryStatus 置为 DELIVERED。
   */
  shipItem(tenantId: number, orderItemId: number, shippingNo: string, logisticsCompany?: string) {
    return request<unknown>({
      url: `/v1/merchant/tenants/${tenantId}/orders/items/${orderItemId}/ship`,
      method: 'post',
      data: { shippingNo, logisticsCompany },
      authRole: 'merchant',
    });
  },

  /**
   * 服务商品核销 - 提交用户出示的核销码后将交付状态置为 CONFIRMED。
   */
  verifyService(tenantId: number, verifyCode: string) {
    return request<unknown>({
      url: `/v1/merchant/tenants/${tenantId}/orders/services/verify`,
      method: 'post',
      data: { verifyCode },
      authRole: 'merchant',
    });
  },
};
