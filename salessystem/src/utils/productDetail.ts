import type { Tenant } from '../types/catalog';

interface InventoryInput {
  stock?: number | null;
  unit?: string | null;
}

export interface InventoryPresentation {
  label: string;
  description: string;
  toneClass: string;
  isOutOfStock: boolean;
}

export interface ProductDetailContractInput extends InventoryInput {
  status?: number | string | null;
  inventoryLabel?: string | null;
  inventoryDescription?: string | null;
  fulfillmentLabel?: string | null;
  fulfillmentDescription?: string | null;
  afterSalesNote?: string | null;
  purchaseLimitNote?: string | null;
  deliveryAccessDescription?: string | null;
  deliveryAccessActionLabel?: string | null;
  purchasable?: boolean | null;
}

export function getProductDetailPresentation(product?: ProductDetailContractInput | null) {
  const inventory = getInventoryPresentation(product || {});
  const active = isActive(product?.status);
  return {
    inventory: {
      ...inventory,
      label: product?.inventoryLabel?.trim() || inventory.label,
      description: product?.inventoryDescription?.trim() || inventory.description,
    },
    fulfillment: {
      label: product?.fulfillmentLabel?.trim() || '到店自提',
      description: product?.fulfillmentDescription?.trim() || '支付成功后生成取货码，商家完成备货后订单完成。',
    },
    deliveryAccess: {
      label: '取货码查看位置',
      description: product?.deliveryAccessDescription?.trim() || '支付成功后可在订单详情中查看取货码。',
      actionLabel: product?.deliveryAccessActionLabel?.trim() || '查看订单',
    },
    afterSalesNote: product?.afterSalesNote?.trim() || '商家确认备货完成后，仍可在订单中发起售后申请。',
    purchaseLimitNote: product?.purchaseLimitNote?.trim() || getPurchaseLimitNote(product || {}),
    saleStatus: {
      label: active && !inventory.isOutOfStock ? '商品可购买' : active ? '商品已售罄' : '商品已下架',
      description: active ? '提交订单时会再次校验门店库存。' : '该商品当前未上架，暂不可购买。',
      toneClass: active && !inventory.isOutOfStock ? 'border-green-100 bg-green-50 text-green-700' : 'border-slate-200 bg-slate-100 text-slate-600',
      isPurchasable: product?.purchasable ?? (active && !inventory.isOutOfStock),
    },
  };
}

export function getInventoryPresentation(product: InventoryInput): InventoryPresentation {
  if (typeof product.stock !== 'number') return { label: '库存待确认', description: '下单前会再次校验门店库存。', toneClass: 'border-slate-200 bg-slate-50 text-slate-600', isOutOfStock: false };
  if (product.stock <= 0) return { label: '暂时缺货', description: '该门店当前没有可售库存。', toneClass: 'border-red-100 bg-red-50 text-red-600', isOutOfStock: true };
  const unit = product.unit?.trim() || '件';
  return { label: product.stock <= 5 ? '库存紧张' : '库存充足', description: `当前可售 ${product.stock} ${unit}`, toneClass: product.stock <= 5 ? 'border-amber-100 bg-amber-50 text-amber-700' : 'border-green-100 bg-green-50 text-green-700', isOutOfStock: false };
}

export function getPurchaseLimitNote(product: InventoryInput) {
  if (typeof product.stock !== 'number') return '库存以结算时校验为准。';
  if (product.stock <= 0) return '当前不可购买，待商家补充库存后可下单。';
  return `单次购买数量不得超过当前门店可售库存 ${product.stock} ${product.unit?.trim() || '件'}。`;
}

export function getMerchantInfoPresentation(tenant?: Tenant | null, _tenantId?: number | null) {
  return { title: tenant?.name || '商户门店', description: tenant?.address || '到店自提请以订单中的门店信息为准。', contactLine: tenant?.phone || '暂无联系电话', actionLabel: '查看商户门店', actionPath: tenant?.id ? `/merchant-store/${tenant.id}` : undefined };
}

function isActive(status?: number | string | null) {
  return status === undefined || status === null || status === 1 || status === '1' || status === 'active';
}
