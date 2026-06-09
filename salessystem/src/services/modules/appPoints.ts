import { request } from '../request';
import type { PointsBalance, PointsLog, ExchangeProduct } from '../../types/points';
import type { PageResult } from '../../types/api';

export const appPointsService = {
  /** 积分余额 */
  getPointsBalance() {
    return request<PointsBalance>({
      url: '/v1/app/tenants/points/balance',
      method: 'get',
      authRole: 'user',
    });
  },

  /** 积分明细（分页） */
  getPointsLogs(tenantId: number, pageNum = 1, pageSize = 20) {
    return request<PageResult<PointsLog>>({
      url: `/v1/app/tenants/${tenantId}/points/logs`,
      method: 'get',
      params: { current: pageNum, size: pageSize },
      authRole: 'user',
    });
  },

  /** 兑换商品列表 */
  getExchangeProducts(tenantId: number) {
    return request<ExchangeProduct[]>({
      url: `/v1/app/tenants/${tenantId}/points/exchange/products`,
      method: 'get',
      authRole: 'user',
    });
  },

  /** 兑换商品 */
  exchangeProduct(tenantId: number, exchangeProductId: number) {
    return request<{ orderNo: string; message: string }>({
      url: `/v1/app/tenants/${tenantId}/points/exchange/${exchangeProductId}`,
      method: 'post',
      authRole: 'user',
    });
  },
};
export default appPointsService;
