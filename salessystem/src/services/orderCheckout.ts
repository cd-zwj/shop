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
  };
}

export function requiresShippingAddress(items: CartItem[]) {
  return items.some((item) => {
    const productType = item.productType;
    return !productType
      || productType === 'PHYSICAL'
      || item.fulfillmentMode === 'EXPRESS_DELIVERY';
  });
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
