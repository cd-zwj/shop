import { request } from '../request';
import type { PageResult } from '../../types/api';
import type {
  AdminMerchantDetail,
  AdminMerchantListItem,
  AdminMerchantPayload,
  AdminMerchantRecord,
  AdminMerchantBalance,
} from '../../types/admin';

export const adminMerchantService = {
  listMerchants(params: {
    current?: number;
    size?: number;
    name?: string;
    status?: number;
  } = {}) {
    return request<PageResult<AdminMerchantListItem>>({
      url: '/v1/admin/merchants',
      method: 'get',
      params: {
        current: params.current ?? 1,
        size: params.size ?? 10,
        name: params.name || undefined,
        status: params.status,
      },
      authRole: 'admin',
    });
  },

  getMerchantDetail(tenantId: number) {
    return request<AdminMerchantDetail>({
      url: `/v1/admin/merchants/${tenantId}`,
      method: 'get',
      authRole: 'admin',
    });
  },

  createMerchant(payload: AdminMerchantPayload) {
    return request<AdminMerchantRecord>({
      url: '/v1/admin/merchants',
      method: 'post',
      data: payload,
      authRole: 'admin',
    });
  },

  updateMerchant(tenantId: number, payload: AdminMerchantPayload) {
    return request<void>({
      url: `/v1/admin/merchants/${tenantId}`,
      method: 'put',
      data: payload,
      authRole: 'admin',
    });
  },

  enableMerchant(tenantId: number) {
    return request<void>({
      url: `/v1/admin/merchants/${tenantId}/enable`,
      method: 'put',
      authRole: 'admin',
    });
  },

  disableMerchant(tenantId: number) {
    return request<void>({
      url: `/v1/admin/merchants/${tenantId}/disable`,
      method: 'put',
      authRole: 'admin',
    });
  },

  getMerchantBalance(tenantId: number) {
    return request<AdminMerchantBalance>({
      url: `/v1/admin/merchants/${tenantId}/balance`,
      method: 'get',
      authRole: 'admin',
    });
  },
};
