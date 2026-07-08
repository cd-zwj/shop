import { beforeEach, describe, expect, it, vi } from 'vitest';

const mockRequest = vi.fn();
vi.mock('../request', () => ({
  request: (...args: unknown[]) => mockRequest(...args),
}));

import { merchantEmployeeService } from './merchantEmployee';

beforeEach(() => {
  vi.clearAllMocks();
});

describe('merchantEmployeeService', () => {
  it('loads employees with merchant auth', async () => {
    const employees = [{ id: 1, platformUserId: 10, employeeRole: 'OWNER', status: 1 }];
    mockRequest.mockResolvedValue(employees);

    const result = await merchantEmployeeService.listEmployees(2);

    expect(mockRequest).toHaveBeenCalledWith({
      url: '/v1/merchant/tenants/2/employees',
      method: 'get',
      authRole: 'merchant',
    });
    expect(result).toEqual(employees);
  });

  it('updates employee role with merchant auth', async () => {
    mockRequest.mockResolvedValue({ id: 3, employeeRole: 'FINANCE' });

    await merchantEmployeeService.updateRole(2, 3, 'FINANCE');

    expect(mockRequest).toHaveBeenCalledWith({
      url: '/v1/merchant/tenants/2/employees/3/role',
      method: 'put',
      data: { employeeRole: 'FINANCE' },
      authRole: 'merchant',
    });
  });
});
