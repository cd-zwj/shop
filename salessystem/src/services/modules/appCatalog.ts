import { request } from '../request';
import type { AppStore, Product, Tenant } from '../../types/catalog';

export const appCatalogService = {
  listTenants() {
    return request<Tenant[]>({
      url: '/v1/app/tenants',
      method: 'get',
      authRole: false,
    });
  },

  getTenant(tenantId: number) {
    return request<Tenant>({
      url: `/v1/app/tenants/${tenantId}`,
      method: 'get',
      authRole: false,
    });
  },

  listTenantStores(tenantId: number) {
    return request<AppStore[]>({
      url: `/v1/app/tenants/${tenantId}/stores`,
      method: 'get',
      authRole: false,
    });
  },

  listTenantProducts(tenantId: number, storeId: number) {
    return request<Product[]>({
      url: `/v1/app/tenants/${tenantId}/products?storeId=${encodeURIComponent(storeId)}`,
      method: 'get',
      authRole: false,
    });
  },

  getProduct(productId: number, storeId: number) {
    return request<Product>({
      url: `/v1/app/products/${productId}?storeId=${encodeURIComponent(storeId)}`,
      method: 'get',
      authRole: false,
    });
  },
};

