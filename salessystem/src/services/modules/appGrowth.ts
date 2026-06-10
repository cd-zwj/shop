import { request } from '../request';
import type { GrowthOverview, GrowthLog } from '../../types/growth';
import type { PageResult } from '../../types/api';

export const appGrowthService = {
  /** 成长值概览（总额 + 等级 + 下一级阈值） */
  getGrowthOverview(tenantId: number) {
    return request<GrowthOverview>({
      url: `/v1/app/tenants/${tenantId}/growth`,
      method: 'get',
      authRole: 'user',
    });
  },

  /** 成长值变动日志（分页） */
  getGrowthLogs(tenantId: number, pageNum = 1, pageSize = 20) {
    return request<PageResult<GrowthLog>>({
      url: `/v1/app/tenants/${tenantId}/growth/logs`,
      method: 'get',
      params: { current: pageNum, size: pageSize },
      authRole: 'user',
    });
  },
};

export default appGrowthService;
