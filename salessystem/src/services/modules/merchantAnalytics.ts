import { request } from '../request';
import type { MerchantAnalyticsFilters, MerchantProductSalesRankItem } from '../../types/merchant';

export const merchantAnalyticsService = {
  getProductSalesRank(tenantId: number, filters: MerchantAnalyticsFilters = {}) {
    return request<MerchantProductSalesRankItem[]>({
      url: `/v1/merchant/tenants/${tenantId}/analytics/product-rank`,
      method: 'get',
      params: {
        startDate: filters.startDate,
        endDate: filters.endDate,
        limit: filters.limit ?? 5,
      },
      authRole: 'merchant',
    });
  },
};
