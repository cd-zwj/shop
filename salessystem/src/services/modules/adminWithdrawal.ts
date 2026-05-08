import { request } from '../request';
import type { PageResult } from '../../types/api';
import type { AdminWithdrawal } from '../../types/admin';

export const adminWithdrawalService = {
  listWithdrawals(params: {
    current?: number;
    size?: number;
    merchantName?: string;
    status?: number;
    startDate?: string;
    endDate?: string;
  } = {}) {
    return request<PageResult<AdminWithdrawal>>({
      url: '/v1/admin/withdrawals',
      method: 'get',
      params: {
        current: params.current ?? 1,
        size: params.size ?? 10,
        merchantName: params.merchantName || undefined,
        status: params.status,
        startDate: params.startDate || undefined,
        endDate: params.endDate || undefined,
      },
      authRole: 'admin',
    });
  },

  approveWithdrawal(withdrawalId: number) {
    return request<void>({
      url: `/v1/admin/withdrawals/${withdrawalId}/approve`,
      method: 'put',
      authRole: 'admin',
    });
  },

  rejectWithdrawal(withdrawalId: number, reason: string) {
    return request<void>({
      url: `/v1/admin/withdrawals/${withdrawalId}/reject`,
      method: 'put',
      data: { reason },
      authRole: 'admin',
    });
  },
};
