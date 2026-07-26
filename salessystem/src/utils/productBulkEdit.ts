import type { MerchantProduct, MerchantProductUpsertPayload } from '../types/merchant';

export const MAX_PRODUCT_STOCK = 999999;
export type ProductEditableStatus = 'active' | 'inactive' | 'out_of_stock';
export type ProductStatusFilter = ProductEditableStatus | 'ALL';

export const PRODUCT_STATUS_FILTERS: Array<{ id: ProductStatusFilter; label: string }> = [
  { id: 'ALL', label: '全部' },
  { id: 'active', label: '上架中' },
  { id: 'inactive', label: '已下架' },
  { id: 'out_of_stock', label: '已售罄' },
];

export function normalizeProductStatusFilter(value: string | null): ProductStatusFilter {
  return value === 'active' || value === 'inactive' || value === 'out_of_stock' ? value : 'ALL';
}

export function buildProductStatusUpdatePayload(product: MerchantProduct, status: ProductEditableStatus): MerchantProductUpsertPayload {
  return toPayload(product, { status });
}

export function buildProductStockUpdatePayload(product: MerchantProduct, stock: number): MerchantProductUpsertPayload {
  return toPayload(product, { stock, status: stock <= 0 ? 'out_of_stock' : product.status === 'inactive' ? 'inactive' : 'active' });
}

function toPayload(product: MerchantProduct, changes: Partial<MerchantProductUpsertPayload>): MerchantProductUpsertPayload {
  return {
    productCode: product.productCode || undefined,
    name: product.name,
    price: Number(product.price),
    unit: product.unit || undefined,
    category: product.category || undefined,
    description: product.description || undefined,
    imageUrl: product.imageUrl || undefined,
    storeId: product.storeId || undefined,
    fulfillmentMode: 'STORE_PICKUP',
    stock: Number(product.stock || 0),
    status: product.status as ProductEditableStatus,
    ...changes,
  };
}
