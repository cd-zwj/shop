import type { MerchantProduct, MerchantProductUpsertPayload } from '../types/merchant';

export type ProductStatusFilter = 'ALL' | 'active' | 'inactive' | 'out_of_stock';
export type ProductEditableStatus = Exclude<ProductStatusFilter, 'ALL'>;

export const PRODUCT_STATUS_FILTERS: Array<{ id: ProductStatusFilter; label: string }> = [
  { id: 'ALL', label: '全部' },
  { id: 'active', label: '上架' },
  { id: 'inactive', label: '下架' },
  { id: 'out_of_stock', label: '售罄' },
];

export function normalizeProductStatusFilter(status: string | null): ProductStatusFilter {
  return PRODUCT_STATUS_FILTERS.some((item) => item.id === status) ? status as ProductStatusFilter : 'ALL';
}

export function buildProductStatusUpdatePayload(
  product: MerchantProduct,
  status: ProductEditableStatus,
): MerchantProductUpsertPayload {
  return buildProductUpdatePayload(product, { status });
}

export function buildProductStockUpdatePayload(
  product: MerchantProduct,
  stock: number,
): MerchantProductUpsertPayload {
  if (stock < 0) {
    throw new Error('库存不能小于 0');
  }
  return buildProductUpdatePayload(product, { stock });
}

function buildProductUpdatePayload(
  product: MerchantProduct,
  overrides: Partial<Pick<MerchantProductUpsertPayload, 'status' | 'stock'>>,
): MerchantProductUpsertPayload {
  return {
    productCode: product.productCode || undefined,
    name: product.name,
    price: Number(product.price || 0),
    unit: product.unit || undefined,
    category: product.category || undefined,
    description: product.description || undefined,
    imageUrl: product.imageUrl || undefined,
    storeId: product.storeId || undefined,
    fulfillmentMode: product.fulfillmentMode || undefined,
    virtualTypeId: product.virtualTypeId || undefined,
    virtualCategoryId: product.virtualCategoryId || undefined,
    stock: Number(product.stock || 0),
    status: product.status === 'active' || product.status === 'inactive' || product.status === 'out_of_stock'
      ? product.status
      : 'active',
    productType: product.productType || undefined,
    deliveryConfig: product.deliveryConfig || undefined,
    ...overrides,
  };
}
