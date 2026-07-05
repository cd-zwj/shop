import type { SalesOrderItem } from '../types/order';

export type FulfillmentTone = 'neutral' | 'warning' | 'success' | 'danger';

export interface OrderItemFulfillmentPresentation {
  label: string;
  description: string;
  tone: FulfillmentTone;
  actionLabel?: string;
  actionPath?: string;
}

const VIRTUAL_TYPES = new Set(['VIRTUAL', 'CARD_KEY', 'SERVICE', 'SUBSCRIPTION']);

export function getOrderItemFulfillmentPresentation(item: SalesOrderItem): OrderItemFulfillmentPresentation {
  const productType = item.productType ?? 'PHYSICAL';
  const status = item.deliveryStatus ?? 'PENDING';
  const action = buildPurchaseAction(item.orderNo, status, productType);

  if (status === 'FAILED' || status === 'REVOKE_FAILED') {
    return {
      label: status === 'REVOKE_FAILED' ? '撤销失败' : '交付失败',
      description: `${getProductTypeLabel(productType)}交付失败，请联系商户处理或稍后刷新履约记录。`,
      tone: 'danger',
      ...action,
    };
  }

  if (status === 'REVOKED') {
    return {
      label: '已撤销',
      description: '该商品交付已因退款或售后撤销。',
      tone: 'neutral',
      ...action,
    };
  }

  if (productType === 'PHYSICAL') {
    return getPhysicalPresentation(status, action);
  }

  if (VIRTUAL_TYPES.has(productType)) {
    return getVirtualPresentation(productType, status, action);
  }

  return {
    label: '履约中',
    description: '商品正在履约处理中，可稍后刷新订单或前往我的已购查看记录。',
    tone: 'warning',
    ...action,
  };
}

function getPhysicalPresentation(
  status: string,
  action: Pick<OrderItemFulfillmentPresentation, 'actionLabel' | 'actionPath'>,
): OrderItemFulfillmentPresentation {
  if (status === 'DELIVERED') {
    return {
      label: '已发货',
      description: '物流信息已生成，可前往我的已购查看物流单号并确认收货。',
      tone: 'success',
      ...action,
    };
  }

  if (status === 'CONFIRMED') {
    return {
      label: '已签收',
      description: '你已确认收货，该订单的实物履约已完成。',
      tone: 'success',
      ...action,
    };
  }

  if (status === 'DELIVERING') {
    return {
      label: '发货中',
      description: '商户正在处理发货，物流单号生成后可在我的已购中查看。',
      tone: 'warning',
      ...action,
    };
  }

  return {
    label: '待发货',
    description: '商户尚未填写物流信息，发货后可在订单和我的已购中查看。',
    tone: 'warning',
  };
}

function getVirtualPresentation(
  productType: string,
  status: string,
  action: Pick<OrderItemFulfillmentPresentation, 'actionLabel' | 'actionPath'>,
): OrderItemFulfillmentPresentation {
  if (status === 'DELIVERED' || status === 'CONFIRMED') {
    return {
      label: status === 'CONFIRMED' ? '已确认' : '已交付',
      description: `${getVirtualDeliveredDescription(productType)}`,
      tone: 'success',
      ...action,
    };
  }

  if (status === 'DELIVERING') {
    return {
      label: '交付中',
      description: '系统正在准备虚拟交付内容，请稍后刷新或前往我的已购查看。',
      tone: 'warning',
      ...action,
    };
  }

  return {
    label: '待交付',
    description: '支付完成后系统会生成交付记录，可在我的已购中查看。',
    tone: 'warning',
  };
}

function getVirtualDeliveredDescription(productType: string): string {
  if (productType === 'CARD_KEY') {
    return '兑换码已发放，可在我的已购中重新查看或复制。';
  }

  if (productType === 'SERVICE') {
    return '服务核销码已发放，可在我的已购中重新查看。';
  }

  if (productType === 'SUBSCRIPTION') {
    return '订阅权益已激活，可在我的已购中查看有效期。';
  }

  return '虚拟内容已发放，可在我的已购中重新查看文件、链接或账号信息。';
}

function buildPurchaseAction(
  orderNo: string | null | undefined,
  status: string,
  productType: string,
): Pick<OrderItemFulfillmentPresentation, 'actionLabel' | 'actionPath'> {
  if (!orderNo || status === 'PENDING') {
    return {};
  }

  return {
    actionLabel: VIRTUAL_TYPES.has(productType) && (status === 'DELIVERED' || status === 'CONFIRMED')
      ? '查看交付内容'
      : '查看履约记录',
    actionPath: `/purchases?orderNo=${encodeURIComponent(orderNo)}`,
  };
}

function getProductTypeLabel(productType: string): string {
  if (productType === 'CARD_KEY') return '兑换码';
  if (productType === 'SERVICE') return '服务';
  if (productType === 'SUBSCRIPTION') return '订阅';
  if (productType === 'VIRTUAL') return '虚拟内容';
  return '商品';
}
