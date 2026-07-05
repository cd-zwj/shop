import { describe, expect, it, vi } from 'vitest';
import type { CartItem } from '../types/cart';
import { validateCartItemsAgainstCatalog } from './cartValidation';

const baseItem: CartItem = {
  productId: 10,
  tenantId: 2,
  name: '旧商品名',
  price: 1200,
  quantity: 3,
  imageUrl: null,
  stock: 10,
  category: '旧分类',
};

describe('cartValidation', () => {
  it('refreshes price and stock changes before checkout', async () => {
    const loadProduct = vi.fn().mockResolvedValue({
      id: 10,
      tenantId: 2,
      name: '新商品名',
      price: 1500,
      stock: 2,
      status: 1,
      category: '新分类',
      imageUrl: 'https://example.com/new.png',
    });

    const result = await validateCartItemsAgainstCatalog([baseItem], loadProduct);

    expect(result.hasIssues).toBe(true);
    expect(result.hasBlockingIssues).toBe(false);
    expect(result.refreshedItems[0]).toMatchObject({
      name: '新商品名',
      price: 1500,
      quantity: 2,
      stock: 2,
      category: '新分类',
    });
    expect(result.issues.map((item) => item.message)).toEqual([
      '新商品名 价格已从 ¥1,200.00 调整为 ¥1,500.00',
      '新商品名 当前库存仅剩 2 件，已自动调整购买数量',
    ]);
  });

  it('warns when product fulfillment changes before checkout', async () => {
    const loadProduct = vi.fn().mockResolvedValue({
      id: 10,
      tenantId: 2,
      name: '电子兑换券',
      price: 1200,
      stock: 10,
      status: 1,
      category: '虚拟商品',
      imageUrl: null,
      productType: 'CARD_KEY',
      fulfillmentMode: 'ONLINE_VIRTUAL',
    });

    const result = await validateCartItemsAgainstCatalog([
      { ...baseItem, productType: 'PHYSICAL', fulfillmentMode: 'EXPRESS_DELIVERY' },
    ], loadProduct);

    expect(result.hasIssues).toBe(true);
    expect(result.hasBlockingIssues).toBe(false);
    expect(result.refreshedItems[0]).toMatchObject({
      productType: 'CARD_KEY',
      fulfillmentMode: 'ONLINE_VIRTUAL',
    });
    expect(result.issues.map((item) => item.message)).toContain(
      '电子兑换券 交付方式已变化，请重新确认是否需要收货地址或虚拟交付说明',
    );
  });

  it('blocks checkout when product is unavailable', async () => {
    const loadProduct = vi.fn().mockResolvedValue(null);

    const result = await validateCartItemsAgainstCatalog([baseItem], loadProduct);

    expect(result.hasBlockingIssues).toBe(true);
    expect(result.refreshedItems).toEqual([]);
    expect(result.issues[0].message).toBe('旧商品名 已下架或不存在，已从购物车移除');
  });
});
