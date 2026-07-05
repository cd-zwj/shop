import { describe, expect, it } from 'vitest';
import { buildOrderPayload, requiresShippingAddress } from './orderCheckout';
import type { CartItem } from '../types/cart';

const baseItem: CartItem = {
  productId: 1,
  tenantId: 9,
  name: '纸质书',
  price: 39,
  quantity: 2,
  productType: 'PHYSICAL',
  fulfillmentMode: 'EXPRESS_DELIVERY',
};

describe('orderCheckout', () => {
  it('adds selected shipping address id to create order payload', () => {
    const payload = buildOrderPayload(
      [baseItem],
      'APP_CART',
      undefined,
      'NO_WALLET',
      'ALIPAY_PAGE',
      55,
    );

    expect(payload.addressId).toBe(55);
    expect(payload.items).toEqual([{ productId: 1, quantity: 2, price: 39 }]);
  });

  it('requires shipping address for physical or legacy unknown product types only', () => {
    expect(requiresShippingAddress([baseItem])).toBe(true);
    expect(requiresShippingAddress([{ ...baseItem, productType: null, fulfillmentMode: null }])).toBe(true);
    expect(requiresShippingAddress([{ ...baseItem, productType: 'CARD_KEY', fulfillmentMode: 'ONLINE_VIRTUAL' }])).toBe(false);
  });
});
