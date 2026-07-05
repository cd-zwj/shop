import { describe, expect, it } from 'vitest';
import {
  getAfterSalesNote,
  getFulfillmentPresentation,
  getInventoryPresentation,
  getPurchaseLimitNote,
} from './productDetail';

describe('productDetail helpers', () => {
  it('describes available stock when backend provides quantity', () => {
    const presentation = getInventoryPresentation({ stock: 6, unit: '件' });

    expect(presentation.label).toBe('库存充足');
    expect(presentation.description).toBe('当前可售 6 件');
    expect(presentation.isOutOfStock).toBe(false);
  });

  it('marks zero stock as unavailable', () => {
    const presentation = getInventoryPresentation({ stock: 0 });

    expect(presentation.label).toBe('暂时缺货');
    expect(presentation.isOutOfStock).toBe(true);
  });

  it('describes virtual delivery and after-sales guidance', () => {
    expect(getFulfillmentPresentation('ONLINE_VIRTUAL', 'CARD_KEY').description).toContain('支付成功后');
    expect(getAfterSalesNote('ONLINE_VIRTUAL', 'CARD_KEY')).toContain('未使用');
  });

  it('falls back to checkout validation when stock is unknown', () => {
    expect(getPurchaseLimitNote({ stock: null })).toBe('库存以结算时校验为准。');
  });
});
