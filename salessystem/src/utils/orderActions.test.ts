import { describe, expect, it } from 'vitest';
import { buildRepurchaseCartItems, getPaymentFailureActions } from './orderActions';

describe('orderActions', () => {
  it('ends a failed order and directs the user to repurchase', () => {
    expect(getPaymentFailureActions('failed', 'SO202607060001')).toEqual({
      primaryLabel: '查看订单详情',
      showRepurchase: true,
      showRetryPayment: false,
    });
  });

  it('keeps store scope and fen prices when rebuilding pickup cart items', () => {
    const items = buildRepurchaseCartItems({
      order: {
        id: 1,
        orderNo: 'SO-1',
        tenantId: 9,
        platformUserId: 3,
        orderStatus: 'CLOSED',
        payStatus: 'FAILED',
        totalAmount: 2880,
        storeId: 66,
        fulfillmentMode: 'STORE_PICKUP',
      },
      items: [{
        id: 2,
        orderId: 1,
        orderNo: 'SO-1',
        tenantId: 9,
        productId: 88,
        productName: '实体商品',
        price: 2880,
        quantity: 2,
        subtotal: 5760,
      }],
    });

    expect(items).toEqual([expect.objectContaining({
      productId: 88,
      tenantId: 9,
      storeId: 66,
      fulfillmentMode: 'STORE_PICKUP',
      price: 2880,
      quantity: 2,
    })]);
  });

  it('offers direct retry when an order payment bill is closed or expired', () => {
    expect(getPaymentFailureActions('closed', 'SO202607060001')).toMatchObject({
      primaryLabel: '重新支付',
      showRetryPayment: true,
    });
    expect(getPaymentFailureActions('expired', 'SO202607060001')).toMatchObject({
      primaryLabel: '重新支付',
      showRetryPayment: true,
    });
  });

  it('does not offer retry when the payment status is still pending', () => {
    expect(getPaymentFailureActions('pending', 'SO202607060001')).toEqual({
      primaryLabel: '查看订单详情',
      showRepurchase: false,
      showRetryPayment: false,
    });
  });

  it('falls back to the order list when no order number is available', () => {
    expect(getPaymentFailureActions('failed', null)).toEqual({
      primaryLabel: '返回订单列表',
      showRepurchase: false,
      showRetryPayment: false,
    });
  });
});
