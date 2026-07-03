import { request } from '../request';
import type {
  VirtualProductCategory,
  VirtualProductCategoryPayload,
  VirtualProductType,
  VirtualProductTypePayload,
} from '../../types/merchant';

export const merchantProductTaxonomyService = {
  listTypes(tenantId: number, status?: number) {
    return request<VirtualProductType[]>({
      url: `/v1/merchant/tenants/${tenantId}/virtual-product-types`,
      method: 'get',
      params: { status },
      authRole: 'merchant',
    });
  },

  createType(tenantId: number, payload: VirtualProductTypePayload) {
    return request<VirtualProductType>({
      url: `/v1/merchant/tenants/${tenantId}/virtual-product-types`,
      method: 'post',
      data: payload,
      authRole: 'merchant',
    });
  },

  updateType(tenantId: number, id: number, payload: VirtualProductTypePayload) {
    return request<VirtualProductType>({
      url: `/v1/merchant/tenants/${tenantId}/virtual-product-types/${id}`,
      method: 'put',
      data: payload,
      authRole: 'merchant',
    });
  },

  deleteType(tenantId: number, id: number) {
    return request<void>({
      url: `/v1/merchant/tenants/${tenantId}/virtual-product-types/${id}`,
      method: 'delete',
      authRole: 'merchant',
    });
  },

  listCategories(tenantId: number, filters: { typeId?: number; status?: number } = {}) {
    return request<VirtualProductCategory[]>({
      url: `/v1/merchant/tenants/${tenantId}/virtual-product-categories`,
      method: 'get',
      params: {
        typeId: filters.typeId,
        status: filters.status,
      },
      authRole: 'merchant',
    });
  },

  createCategory(tenantId: number, payload: VirtualProductCategoryPayload) {
    return request<VirtualProductCategory>({
      url: `/v1/merchant/tenants/${tenantId}/virtual-product-categories`,
      method: 'post',
      data: payload,
      authRole: 'merchant',
    });
  },

  updateCategory(tenantId: number, id: number, payload: VirtualProductCategoryPayload) {
    return request<VirtualProductCategory>({
      url: `/v1/merchant/tenants/${tenantId}/virtual-product-categories/${id}`,
      method: 'put',
      data: payload,
      authRole: 'merchant',
    });
  },

  deleteCategory(tenantId: number, id: number) {
    return request<void>({
      url: `/v1/merchant/tenants/${tenantId}/virtual-product-categories/${id}`,
      method: 'delete',
      authRole: 'merchant',
    });
  },
};
