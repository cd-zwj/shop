import { describe, expect, it } from 'vitest';
import { getPaymentFailureActions } from './orderActions';

describe('orderActions', () => {
  it('offers direct retry when an order payment fails', () => {
    expect(getPaymentFailureActions('failed', 'SO202607060001')).toEqual({
      primaryLabel: '重新支付',
      showRepurchase: true,
      showRetryPayment: true,
    });
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
