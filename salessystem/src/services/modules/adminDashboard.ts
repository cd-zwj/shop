import { request } from '../request';
import type { AdminDashboardOverview, AdminInfo } from '../../types/admin';

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
};
