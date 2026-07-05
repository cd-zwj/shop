import { request } from '../request';
import type { PageResult } from '../../types/api';
import type {
  AppCreateOrderPayload,
  OrderPayment,
  SalesOrder,
  SalesOrderDetail,
} from '../../types/order';
import { normalizeSalesOrderDetail } from '../../utils/orderLifecycle';

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

  async getOrder(orderNo: string) {
    const result = await request<SalesOrderDetail>({
      url: `/v1/app/orders/${orderNo}`,
      method: 'get',
      authRole: 'user',
    });
    return normalizeSalesOrderDetail(result);
  },

  repayOrder(orderNo: string, paymentChannelCode: 'ALIPAY_PAGE' | 'EXT_PROVIDER' = 'ALIPAY_PAGE') {
    return request<OrderPayment>({
      url: `/v1/app/orders/${orderNo}/repay`,
      method: 'post',
      params: { paymentChannelCode },
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
