import { appOrderService } from './modules/appOrder';
import type { OrderPayment, AppCreateOrderPayload, WalletStrategy } from '../types/order';
import type { PaymentChannelCode } from '../types/payment';
import type { CartItem, CheckoutSource } from '../types/cart';

export function buildOrderPayload(
  items: CartItem[],
  source: CheckoutSource,
  selectedUserCouponId: number | undefined,
  walletStrategy: WalletStrategy,
  paymentChannelCode: PaymentChannelCode | undefined,
  addressId?: number,
): AppCreateOrderPayload {
  if (items.length === 0) {
    throw new Error('购物车为空，无法创建订单');
  }

  const tenantId = items[0].tenantId;
  const hasMultipleTenants = items.some((item) => item.tenantId !== tenantId);

  if (hasMultipleTenants) {
    throw new Error('当前仅支持按单个商户提交订单');
  }

  const isStorePickup = items.every((item) => item.fulfillmentMode === 'STORE_PICKUP');
  const pickupStoreIds = [...new Set(items.map((item) => item.storeId).filter((storeId): storeId is number => typeof storeId === 'number'))];
  if (!isStorePickup || pickupStoreIds.length !== 1 || items.some((item) => typeof item.storeId !== 'number')) {
    throw new Error('到店自提商品必须绑定同一个门店');
  }

  return {
    tenantId,
    totalAmount: items.reduce((sum, item) => sum + item.price * item.quantity, 0),
    subject: items.length === 1 ? items[0].name : `共 ${items.length} 件商品`,
    source,
    items: items.map((item) => ({
      productId: item.productId,
      quantity: item.quantity,
      price: item.price,
    })),
    walletStrategy,
    paymentChannelCode,
    selectedUserCouponId,
    addressId,
    fulfillmentMode: isStorePickup ? 'STORE_PICKUP' : undefined,
    storeId: isStorePickup ? pickupStoreIds[0] : undefined,
  };
}

export function requiresShippingAddress(_items: CartItem[]) {
  return false;
}

export function createOrderForItems(
  items: CartItem[],
  source: CheckoutSource,
  selectedUserCouponId: number | undefined,
  walletStrategy: WalletStrategy,
  paymentChannelCode: PaymentChannelCode | undefined,
  addressId?: number,
): Promise<OrderPayment> {
  return appOrderService.createOrder(buildOrderPayload(items, source, selectedUserCouponId, walletStrategy, paymentChannelCode, addressId));
}

export function getOrderCheckoutPath(payment: OrderPayment) {
  if (payment.paymentBillNo) {
    const reusedPaymentBill =
      typeof payment.reusedPaymentBill === 'boolean'
        ? `&reused=${payment.reusedPaymentBill ? '1' : '0'}`
        : '';
    return `/payment/status?billNo=${encodeURIComponent(payment.paymentBillNo)}&orderNo=${encodeURIComponent(payment.orderNo)}&source=order${reusedPaymentBill}`;
  }

  return `/order/${encodeURIComponent(payment.orderNo)}`;
}
