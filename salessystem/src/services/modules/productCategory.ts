import { request } from '../request';

/**
 * 商品分类节点（树形结构）
 */
export interface ProductCategory {
  id: number;
  tenantId: number | null;
  name: string;
  parentId: number;
  sortOrder: number | null;
  icon: string | null;
  status: number;
  children?: ProductCategory[];
}

/**
 * 商品分类 service。
 *
 * 后端 V1AppCatalogController 尚未暴露分类接口，以下路径为预设路径，
 * 与后端现有 URL 命名风格一致。当后端实现后可直接使用，无需修改前端。
 */
export const productCategoryService = {
  /**
   * 获取商品分类列表（树形）。
   * @param tenantId 可选，传入则返回该商户的分类，不传返回平台级分类。
   */
  listCategories(tenantId?: number) {
    if (tenantId) {
      return request<ProductCategory[]>({
        url: `/v1/app/tenants/${tenantId}/product-categories`,
        method: 'get',
        authRole: false,
      });
    }
    return request<ProductCategory[]>({
      url: '/v1/app/product-categories',
      method: 'get',
      authRole: false,
    });
  },
};
