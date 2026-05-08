import { request } from '../request';
import type { PageResult } from '../../types/api';
import type {
  MerchantProduct,
  MerchantProductFilters,
  MerchantProductUpsertPayload,
} from '../../types/merchant';

export const merchantProductService = {
  listProducts(tenantId: number, filters: MerchantProductFilters = {}) {
    return request<PageResult<MerchantProduct>>({
      url: `/v1/merchant/tenants/${tenantId}/products`,
      method: 'get',
      params: {
        current: filters.current ?? 1,
        size: filters.size ?? 10,
        search: filters.search || undefined,
        category: filters.category || undefined,
        status: filters.status || undefined,
      },
      authRole: 'merchant',
    });
  },

  getProduct(tenantId: number, productId: number) {
    return request<MerchantProduct>({
      url: `/v1/merchant/tenants/${tenantId}/products/${productId}`,
      method: 'get',
      authRole: 'merchant',
    });
  },

  createProduct(tenantId: number, payload: MerchantProductUpsertPayload) {
    return request<MerchantProduct>({
      url: `/v1/merchant/tenants/${tenantId}/products`,
      method: 'post',
      data: payload,
      authRole: 'merchant',
    });
  },

  updateProduct(tenantId: number, productId: number, payload: MerchantProductUpsertPayload) {
    return request<MerchantProduct>({
      url: `/v1/merchant/tenants/${tenantId}/products/${productId}`,
      method: 'put',
      data: payload,
      authRole: 'merchant',
    });
  },

  deleteProduct(tenantId: number, productId: number) {
    return request<void>({
      url: `/v1/merchant/tenants/${tenantId}/products/${productId}`,
      method: 'delete',
      authRole: 'merchant',
    });
  },
};

