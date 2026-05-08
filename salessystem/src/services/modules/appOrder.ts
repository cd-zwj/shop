import { request } from '../request';
import type { PageResult } from '../../types/api';
import type {
  AppCreateOrderPayload,
  OrderPayment,
  SalesOrder,
  SalesOrderDetail,
} from '../../types/order';

export const appOrderService = {
  createOrder(payload: AppCreateOrderPayload) {
    return request<OrderPayment>({
      url: '/v1/app/orders',
      method: 'post',
      data: payload,
      authRole: 'user',
    });
  },

  listOrders(current = 1, size = 10) {
    return request<PageResult<SalesOrder>>({
      url: '/v1/app/orders',
      method: 'get',
      params: { current, size },
      authRole: 'user',
    });
  },

  getOrder(orderNo: string) {
    return request<SalesOrderDetail>({
      url: `/v1/app/orders/${orderNo}`,
      method: 'get',
      authRole: 'user',
    });
  },

  cancelOrder(orderNo: string) {
    return request<void>({
      url: `/v1/app/orders/${orderNo}/cancel`,
      method: 'post',
      authRole: 'user',
    });
  },
};

