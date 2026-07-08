import { request } from '../request';
import type { PageResult } from '../../types/api';
import type { MerchantWorkbenchTask, MerchantWorkbenchTodoSummary } from '../../types/merchant';

export interface MerchantWorkbenchTaskFilters {
  type?: 'compensation' | 'retry' | string;
  pageNum?: number;
  pageSize?: number;
}

export const merchantWorkbenchService = {
  getTodoSummary(tenantId: number) {
    return request<MerchantWorkbenchTodoSummary>({
      url: `/v1/merchant/tenants/${tenantId}/workbench/todos`,
      method: 'get',
      authRole: 'merchant',
    });
  },

  listTasks(tenantId: number, filters: MerchantWorkbenchTaskFilters = {}) {
    return request<PageResult<MerchantWorkbenchTask>>({
      url: `/v1/merchant/tenants/${tenantId}/workbench/tasks`,
      method: 'get',
      params: {
        type: filters.type ?? 'compensation',
        pageNum: filters.pageNum ?? 1,
        pageSize: filters.pageSize ?? 20,
      },
      authRole: 'merchant',
    });
  },
};
