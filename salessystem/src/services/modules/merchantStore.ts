import { request } from '../request';
import type { PageResult } from '../../types/api';
import type { MerchantStore, MerchantStorePayload } from '../../types/merchant';

export const merchantStoreService = {
  listStores(
    tenantId: number,
    filters: { current?: number; size?: number; keyword?: string; status?: number } = {},
  ) {
    return request<PageResult<MerchantStore>>({
      url: `/v1/merchant/tenants/${tenantId}/stores`,
      method: 'get',
      params: {
        current: filters.current ?? 1,
        size: filters.size ?? 20,
        keyword: filters.keyword || undefined,
        status: filters.status,
      },
      authRole: 'merchant',
    });
  },

  createStore(tenantId: number, payload: MerchantStorePayload) {
    return request<MerchantStore>({
      url: `/v1/merchant/tenants/${tenantId}/stores`,
      method: 'post',
      data: payload,
      authRole: 'merchant',
    });
  },

  updateStore(tenantId: number, storeId: number, payload: MerchantStorePayload) {
    return request<MerchantStore>({
      url: `/v1/merchant/tenants/${tenantId}/stores/${storeId}`,
      method: 'put',
      data: payload,
      authRole: 'merchant',
    });
  },

  updateStoreStatus(tenantId: number, storeId: number, status: number) {
    return request<MerchantStore>({
      url: `/v1/merchant/tenants/${tenantId}/stores/${storeId}/status`,
      method: 'put',
      params: { status },
      authRole: 'merchant',
    });
  },

  deleteStore(tenantId: number, storeId: number) {
    return request<void>({
      url: `/v1/merchant/tenants/${tenantId}/stores/${storeId}`,
      method: 'delete',
      authRole: 'merchant',
    });
  },
};
