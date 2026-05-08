import { request } from '../request';
import type { Product, Tenant } from '../../types/catalog';

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

  listTenantProducts(tenantId: number) {
    return request<Product[]>({
      url: `/v1/app/tenants/${tenantId}/products`,
      method: 'get',
      authRole: false,
    });
  },

  getProduct(productId: number) {
    return request<Product>({
      url: `/v1/app/products/${productId}`,
      method: 'get',
      authRole: false,
    });
  },
};

