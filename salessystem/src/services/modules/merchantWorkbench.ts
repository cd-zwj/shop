import { request } from '../request';
import type { MerchantWorkbenchTodoSummary } from '../../types/merchant';

export const merchantWorkbenchService = {
  getTodoSummary(tenantId: number) {
    return request<MerchantWorkbenchTodoSummary>({
      url: `/v1/merchant/tenants/${tenantId}/workbench/todos`,
      method: 'get',
      authRole: 'merchant',
    });
  },
};
