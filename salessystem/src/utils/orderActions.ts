import type { CartItem } from '../types/cart';
import type { SalesOrder, SalesOrderDetail } from '../types/order';
import type { PaymentFlowState } from './paymentStatus';

export function canRepurchaseOrder(order?: SalesOrder | null) {
  if (!order) {
    return false;
  }

  return order.orderStatus === 'CANCELLED'
    || order.orderStatus === 'CLOSED'
    || order.payStatus === 'FAILED'
    || order.payStatus === 'CLOSED';
}

export function buildRepurchaseCartItems(detail?: SalesOrderDetail | null): CartItem[] {
  if (!detail?.order || !detail.items?.length) {
    return [];
  }

  return detail.items.map((item) => ({
    productId: item.productId,
    tenantId: detail.order.tenantId,
    storeId: detail.order.storeId,
    name: item.productName,
    price: item.price,
    quantity: item.quantity,
    imageUrl: null,
    stock: null,
    category: null,
    fulfillmentMode: detail.order.fulfillmentMode,
  }));
}

export function getPaymentFailureActions(state: PaymentFlowState, orderNo?: string | null) {
  if (!orderNo) {
    return {
      primaryLabel: '返回订单列表',
      showRepurchase: false,
      showRetryPayment: false,
    };
  }

  if (state === 'failed') {
    return {
      primaryLabel: '查看订单详情',
      showRepurchase: true,
      showRetryPayment: false,
    };
  }

  if (state === 'closed' || state === 'expired') {
    return {
      primaryLabel: '重新支付',
      showRepurchase: true,
      showRetryPayment: true,
    };
  }

  return {
    primaryLabel: '查看订单详情',
    showRepurchase: false,
    showRetryPayment: false,
  };
}
