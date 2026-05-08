import { request } from '../request';
import type { PageResult } from '../../types/api';
import type {
  AdminOrderDetail,
  AdminOrderListItem,
  AdminPaymentBill,
  AdminRechargeOrder,
  AdminTradeOverview,
} from '../../types/admin';

export const adminTradeService = {
  getOverview() {
    return request<AdminTradeOverview>({
      url: '/v1/admin/trades/overview',
      method: 'get',
      authRole: 'admin',
    });
  },

  listOrders(params: {
    current?: number;
    size?: number;
    orderNo?: string;
    orderStatus?: string;
    payStatus?: string;
    tenantId?: number;
  } = {}) {
    return request<PageResult<AdminOrderListItem>>({
      url: '/v1/admin/orders',
      method: 'get',
      params: {
        current: params.current ?? 1,
        size: params.size ?? 10,
        orderNo: params.orderNo || undefined,
        orderStatus: params.orderStatus || undefined,
        payStatus: params.payStatus || undefined,
        tenantId: params.tenantId,
      },
      authRole: 'admin',
    });
  },

  getOrderDetail(orderNo: string) {
    return request<AdminOrderDetail>({
      url: `/v1/admin/orders/${orderNo}`,
      method: 'get',
      authRole: 'admin',
    });
  },

  listPaymentBills(params: {
    current?: number;
    size?: number;
    bizType?: string;
    payStatus?: string;
    channelCode?: string;
  } = {}) {
    return request<PageResult<AdminPaymentBill>>({
      url: '/v1/admin/payment-bills',
      method: 'get',
      params: {
        current: params.current ?? 1,
        size: params.size ?? 10,
        bizType: params.bizType || undefined,
        payStatus: params.payStatus || undefined,
        channelCode: params.channelCode || undefined,
      },
      authRole: 'admin',
    });
  },

  listRechargeOrders(params: {
    current?: number;
    size?: number;
    walletType?: string;
    bizStatus?: string;
    tenantId?: number;
  } = {}) {
    return request<PageResult<AdminRechargeOrder>>({
      url: '/v1/admin/recharge-orders',
      method: 'get',
      params: {
        current: params.current ?? 1,
        size: params.size ?? 10,
        walletType: params.walletType || undefined,
        bizStatus: params.bizStatus || undefined,
        tenantId: params.tenantId,
      },
      authRole: 'admin',
    });
  },
};
