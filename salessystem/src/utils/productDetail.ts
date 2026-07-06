import type { FulfillmentMode, ProductType, Tenant } from '../types/catalog';

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

export interface DeliveryAccessPresentation {
  label: string;
  description: string;
  actionLabel: string;
}

export interface SaleStatusPresentation {
  label: string;
  description: string;
  toneClass: string;
  isPurchasable: boolean;
}

export interface MerchantInfoPresentation {
  title: string;
  description: string;
  contactLine: string;
  actionLabel: string;
  actionPath?: string;
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

export function getSaleStatusPresentation(status?: number | string | null): SaleStatusPresentation {
  if (status === undefined || status === null || status === 1 || status === '1' || status === 'active') {
    return {
      label: '商品可购买',
      description: '该商品当前处于上架状态，下单前仍会校验价格、库存和履约方式。',
      toneClass: 'border-green-100 bg-green-50 text-green-700',
      isPurchasable: true,
    };
  }

  if (status === 'out_of_stock') {
    return {
      label: '商品已售罄',
      description: '商家已将该商品标记为售罄，暂不可购买。',
      toneClass: 'border-red-100 bg-red-50 text-red-600',
      isPurchasable: false,
    };
  }

  return {
    label: '商品已下架',
    description: '该商品当前未上架，暂不可购买。可进入商户店铺查看其他在售商品。',
    toneClass: 'border-slate-200 bg-slate-100 text-slate-600',
    isPurchasable: false,
  };
}

export function getDeliveryAccessPresentation(
  fulfillmentMode?: FulfillmentMode | string | null,
  productType?: ProductType | string | null,
): DeliveryAccessPresentation {
  if (fulfillmentMode === 'ONLINE_VIRTUAL') {
    if (productType === 'CARD_KEY') {
      return {
        label: '卡密查看位置',
        description: '支付完成并交付成功后，可在“我的已购”中重新查看和复制兑换码。',
        actionLabel: '前往我的已购',
      };
    }

    if (productType === 'SUBSCRIPTION') {
      return {
        label: '权益查看位置',
        description: '支付完成后订阅权益会自动激活，可在“我的已购”查看有效期和交付记录。',
        actionLabel: '查看权益记录',
      };
    }

    return {
      label: '虚拟内容查看位置',
      description: '支付完成后，文件、链接或账号信息会进入“我的已购”，后续可随时重新打开。',
      actionLabel: '查看已购内容',
    };
  }

  if (fulfillmentMode === 'OFFLINE_SERVICE') {
    return {
      label: '服务凭证查看位置',
      description: '支付完成后会生成服务核销凭证，可在“我的已购”中向商户出示或复制核销码。',
      actionLabel: '查看服务凭证',
    };
  }

  return {
    label: '物流查看位置',
    description: '支付完成后，发货进度和物流信息会同步到订单详情和“我的已购”。',
    actionLabel: '查看订单履约',
  };
}

export function getMerchantInfoPresentation(
  tenant?: Pick<Tenant, 'id' | 'name' | 'contact' | 'phone' | 'address'> | null,
  fallbackTenantId?: number | null,
): MerchantInfoPresentation {
  const tenantId = tenant?.id ?? fallbackTenantId ?? undefined;
  const title = tenant?.name?.trim() || (tenantId ? `商户 #${tenantId}` : '商户信息待确认');
  const contactParts = [
    tenant?.contact?.trim() ? `联系人 ${tenant.contact.trim()}` : '',
    tenant?.phone?.trim() || '',
  ].filter(Boolean);
  const address = tenant?.address?.trim();

  return {
    title,
    description: address
      ? `商家地址：${address}`
      : tenantId
        ? '可进入商户店铺查看商家资料、商品和售后联系入口。'
        : '当前商品缺少商户资料，请从商户店铺或商品列表重新进入。',
    contactLine: contactParts.length > 0 ? contactParts.join(' · ') : '联系方式以商户店铺展示为准',
    actionLabel: tenantId ? '进入商户店铺' : '商户信息待确认',
    actionPath: tenantId ? `/merchant-store/${tenantId}` : undefined,
  };
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
