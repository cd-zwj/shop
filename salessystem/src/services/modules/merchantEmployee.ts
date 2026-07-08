import { request } from '../request';
import type { MerchantEmployee, MerchantEmployeeRole } from '../../types/merchant';

export const merchantEmployeeService = {
  listEmployees(tenantId: number) {
    return request<MerchantEmployee[]>({
      url: `/v1/merchant/tenants/${tenantId}/employees`,
      method: 'get',
      authRole: 'merchant',
    });
  },

  addEmployee(tenantId: number, platformUserId: number, employeeRole: MerchantEmployeeRole) {
    return request<MerchantEmployee>({
      url: `/v1/merchant/tenants/${tenantId}/employees`,
      method: 'post',
      data: { platformUserId, employeeRole },
      authRole: 'merchant',
    });
  },

  updateRole(tenantId: number, employeeId: number, employeeRole: MerchantEmployeeRole) {
    return request<MerchantEmployee>({
      url: `/v1/merchant/tenants/${tenantId}/employees/${employeeId}/role`,
      method: 'put',
      data: { employeeRole },
      authRole: 'merchant',
    });
  },

  updateStatus(tenantId: number, employeeId: number, status: 0 | 1) {
    return request<MerchantEmployee>({
      url: `/v1/merchant/tenants/${tenantId}/employees/${employeeId}/status`,
      method: 'put',
      data: { status },
      authRole: 'merchant',
    });
  },
};
