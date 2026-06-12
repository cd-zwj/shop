import { request } from '../request';
import type { AdminDashboardOverview, AdminInfo, AdminTrendData } from '../../types/admin';

export interface TrendQueryParams {
  startDate?: string;
  endDate?: string;
  granularity?: 'DAY' | 'WEEK' | 'MONTH';
}

export const adminDashboardService = {
  getInfo() {
    return request<AdminInfo>({
      url: '/v1/admin/info',
      method: 'get',
      authRole: 'admin',
    });
  },

  getOverview() {
    return request<AdminDashboardOverview>({
      url: '/v1/admin/dashboard/overview',
      method: 'get',
      authRole: 'admin',
    });
  },

  getTrend(params: TrendQueryParams = {}) {
    return request<AdminTrendData>({
      url: '/v1/admin/dashboard/trend',
      method: 'get',
      params,
      authRole: 'admin',
    });
  },
};
