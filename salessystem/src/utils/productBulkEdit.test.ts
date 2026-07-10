import { describe, expect, it } from 'vitest';
import {
  buildProductStockUpdatePayload,
  buildProductStatusUpdatePayload,
  normalizeProductStatusFilter,
} from './productBulkEdit';
import type { MerchantProduct } from '../types/merchant';

const product: MerchantProduct = {
  id: 1,
  tenantId: 10,
  productCode: 'SKU-001',
  name: '会员兑换码',
  price: 9900,
  unit: '张',
  category: '会员',
  description: '可兑换会员权益',
  imageUrl: 'https://example.com/cover.png',
  storeId: 2,
  fulfillmentMode: 'ONLINE_VIRTUAL',
  virtualTypeId: 3,
  virtualCategoryId: 4,
  stock: 12,
  status: 'active',
  productType: 'CARD_KEY',
  deliveryConfig: '{"note":"card key"}',
};

describe('productBulkEdit', () => {
  it('normalizes invalid status filters to ALL', () => {
    expect(normalizeProductStatusFilter('active')).toBe('active');
    expect(normalizeProductStatusFilter('deleted')).toBe('ALL');
    expect(normalizeProductStatusFilter(null)).toBe('ALL');
  });

  it('builds a full update payload when changing product status', () => {
    const payload = buildProductStatusUpdatePayload(product, 'inactive');

    expect(payload.status).toBe('inactive');
    expect(payload.name).toBe('会员兑换码');
    expect(payload.price).toBe(9900);
    expect(payload.stock).toBe(12);
    expect(payload.productType).toBe('CARD_KEY');
    expect(payload.fulfillmentMode).toBe('ONLINE_VIRTUAL');
  });

  it('sets stock to zero when marking a product out of stock', () => {
    const payload = buildProductStatusUpdatePayload(product, 'out_of_stock');

    expect(payload.status).toBe('out_of_stock');
    expect(payload.stock).toBe(0);
  });

  it('builds a full update payload when adjusting stock', () => {
    const payload = buildProductStockUpdatePayload(product, 28);

    expect(payload.stock).toBe(28);
    expect(payload.status).toBe('active');
    expect(payload.name).toBe('会员兑换码');
    expect(payload.price).toBe(9900);
    expect(payload.productType).toBe('CARD_KEY');
  });

  it('rejects negative stock adjustment values', () => {
    expect(() => buildProductStockUpdatePayload(product, -1)).toThrow('库存不能小于 0');
  });

  it('rejects unusually large stock adjustment values', () => {
    expect(() => buildProductStockUpdatePayload(product, 1000000)).toThrow('库存不能超过 999999');
  });
});
