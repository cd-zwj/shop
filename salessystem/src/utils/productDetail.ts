import type { FulfillmentMode, ProductType } from '../types/catalog';

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

export function getInventoryPresentation(product: InventoryInput): InventoryPresentation {
  if (typeof product.stock !== 'number') {
    return {
      label: '库存待确认',
      description: '下单前会再次校验库存。',
      toneClass: 'border-slate-200 bg-slate-50 text-slate-600',
      isOutOfStock: false,
    };
  }

  if (product.stock <= 0) {
    return {
      label: '暂时缺货',
      description: '该商品当前没有可售库存。',
      toneClass: 'border-red-100 bg-red-50 text-red-600',
      isOutOfStock: true,
    };
  }

  const unit = product.unit?.trim() || '件';
  return {
    label: product.stock <= 5 ? '库存紧张' : '库存充足',
    description: `当前可售 ${product.stock} ${unit}`,
    toneClass: product.stock <= 5
      ? 'border-amber-100 bg-amber-50 text-amber-700'
      : 'border-green-100 bg-green-50 text-green-700',
    isOutOfStock: false,
  };
}

export function getFulfillmentPresentation(
  fulfillmentMode?: FulfillmentMode | string | null,
  productType?: ProductType | string | null,
) {
  if (fulfillmentMode === 'ONLINE_VIRTUAL') {
    return {
      label: getProductTypeLabel(productType),
      description: productType === 'CARD_KEY'
        ? '支付成功后，系统会自动发放可用卡密。'
        : '支付成功后，系统会生成线上交付记录。',
    };
  }

  if (fulfillmentMode === 'OFFLINE_SERVICE') {
    return {
      label: '线下服务',
      description: '支付成功后生成服务凭证，到店或按商家约定核销。',
    };
  }

  return {
    label: '快递发货',
    description: '支付后由商家按订单信息安排发货。',
  };
}

export function getAfterSalesNote(
  fulfillmentMode?: FulfillmentMode | string | null,
  productType?: ProductType | string | null,
) {
  if (fulfillmentMode === 'ONLINE_VIRTUAL') {
    return productType === 'CARD_KEY'
      ? '卡密未使用前可提交售后申请，已使用内容需由商家审核。'
      : '虚拟内容交付后仍可提交售后申请，处理结果以商家审核为准。';
  }

  if (fulfillmentMode === 'OFFLINE_SERVICE') {
    return '服务未核销前可申请售后，已核销订单需商家审核。';
  }

  return '实物商品按订单售后流程处理，退款或退货退款由商家审核。';
}

export function getPurchaseLimitNote(product: InventoryInput) {
  if (typeof product.stock !== 'number') {
    return '库存以结算时校验为准。';
  }

  if (product.stock <= 0) {
    return '当前不可购买，待商家补充库存后可下单。';
  }

  const unit = product.unit?.trim() || '件';
  return `单次立即购买 1 ${unit}，购物车最多不超过当前库存 ${product.stock} ${unit}。`;
}

function getProductTypeLabel(productType?: ProductType | string | null) {
  switch (productType) {
    case 'CARD_KEY':
      return '卡密自动交付';
    case 'SUBSCRIPTION':
      return '订阅权益';
    case 'SERVICE':
      return '服务凭证';
    case 'VIRTUAL':
      return '虚拟商品';
    default:
      return '线上交付';
  }
}
