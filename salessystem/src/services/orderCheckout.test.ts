import { describe, expect, it } from 'vitest';
import { buildOrderPayload, requiresShippingAddress } from './orderCheckout';
import type { CartItem } from '../types/cart';

const pickupItem: CartItem = {
  productId: 101,
  tenantId: 9,
  storeId: 11,
  name: '门店自提商品',
  price: 29.9,
  quantity: 2,
  fulfillmentMode: 'STORE_PICKUP',
};

describe('orderCheckout', () => {
  it('creates a store pickup order without a shipping address', () => {
    const payload = buildOrderPayload(
      [pickupItem],
      'APP_CART',
      undefined,
      'NO_WALLET',
      'ALIPAY_PAGE',
    );

    expect(payload).toMatchObject({
      tenantId: 9,
      storeId: 11,
      fulfillmentMode: 'STORE_PICKUP',
      totalAmount: 59.8,
      items: [{ productId: 101, quantity: 2, price: 29.9 }],
    });
    expect(payload.addressId).toBeUndefined();
    expect(requiresShippingAddress([pickupItem])).toBe(false);
  });

  it('rejects cart items from different pickup stores', () => {
    expect(() => buildOrderPayload(
      [pickupItem, { ...pickupItem, productId: 102, storeId: 12 }],
      'APP_CART',
      undefined,
      'NO_WALLET',
      'ALIPAY_PAGE',
    )).toThrow('到店自提商品必须绑定同一个门店');
  });
});
