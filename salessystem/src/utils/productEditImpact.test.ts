import { describe, expect, it } from 'vitest';
import { buildProductEditImpacts } from './productEditImpact';
import type { MerchantProductUpsertPayload } from '../types/merchant';

const baseForm: MerchantProductUpsertPayload = {
  name: '课程资料包',
  price: 9900,
  stock: 20,
  status: 'active',
  productType: 'VIRTUAL',
  fulfillmentMode: 'ONLINE_VIRTUAL',
};

describe('productEditImpact', () => {
  it('describes display and settlement impacts for every product', () => {
    const impacts = buildProductEditImpacts(baseForm);

    expect(impacts.map((item) => item.key)).toContain('display');
    expect(impacts.map((item) => item.key)).toContain('settlement');
    expect(impacts.find((item) => item.key === 'settlement')?.description).toContain('退款可退金额');
  });

  it('warns when stock is low', () => {
    const impacts = buildProductEditImpacts({ ...baseForm, stock: 3 });

    const inventory = impacts.find((item) => item.key === 'inventory');
    expect(inventory?.tone).toBe('amber');
    expect(inventory?.description).toContain('库存已低于或等于 5');
  });

  it('adds sales status impact when product is not active', () => {
    const impacts = buildProductEditImpacts({ ...baseForm, status: 'inactive' });

    expect(impacts.find((item) => item.key === 'status')?.description).toContain('阻止不可售商品');
  });

  it('uses delivery-specific guidance for card keys and physical products', () => {
    const cardKey = buildProductEditImpacts({ ...baseForm, productType: 'CARD_KEY' })
      .find((item) => item.key === 'fulfillment');
    const physical = buildProductEditImpacts({ ...baseForm, productType: 'PHYSICAL' })
      .find((item) => item.key === 'fulfillment');

    expect(cardKey?.description).toContain('库存池');
    expect(physical?.description).toContain('地址快照');
  });
});
