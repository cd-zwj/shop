import { describe, expect, it } from 'vitest';
import {
  getAfterSalesNote,
  getDeliveryAccessPresentation,
  getFulfillmentPresentation,
  getInventoryPresentation,
  getMerchantInfoPresentation,
  getProductDetailPresentation,
  getPurchaseLimitNote,
  getSaleStatusPresentation,
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

  it('tells users where to view delivered card keys after purchase', () => {
    const presentation = getDeliveryAccessPresentation('ONLINE_VIRTUAL', 'CARD_KEY');

    expect(presentation.label).toBe('卡密查看位置');
    expect(presentation.description).toContain('我的已购');
    expect(presentation.description).toContain('兑换码');
  });

  it('tells users where to reopen virtual files and links', () => {
    const presentation = getDeliveryAccessPresentation('ONLINE_VIRTUAL', 'VIRTUAL');

    expect(presentation.label).toBe('虚拟内容查看位置');
    expect(presentation.description).toContain('文件');
    expect(presentation.actionLabel).toBe('查看已购内容');
  });

  it('tells users where to present service vouchers', () => {
    const presentation = getDeliveryAccessPresentation('OFFLINE_SERVICE', 'SERVICE');

    expect(presentation.label).toBe('服务凭证查看位置');
    expect(presentation.description).toContain('核销码');
  });

  it('falls back to order fulfillment for physical goods', () => {
    const presentation = getDeliveryAccessPresentation('EXPRESS_DELIVERY', 'PHYSICAL');

    expect(presentation.label).toBe('物流查看位置');
    expect(presentation.description).toContain('订单详情');
  });

  it('summarizes merchant contact information when tenant detail is available', () => {
    const presentation = getMerchantInfoPresentation({
      id: 8,
      name: '本地生活旗舰店',
      contact: '张店长',
      phone: '13800000000',
      address: '上海市浦东新区世纪大道 1 号',
    });

    expect(presentation.title).toBe('本地生活旗舰店');
    expect(presentation.description).toContain('上海市浦东新区世纪大道 1 号');
    expect(presentation.contactLine).toBe('联系人 张店长 · 13800000000');
    expect(presentation.actionPath).toBe('/merchant-store/8');
  });

  it('keeps merchant entry actionable when only tenant id is known', () => {
    const presentation = getMerchantInfoPresentation(null, 9);

    expect(presentation.title).toBe('商户 #9');
    expect(presentation.description).toContain('商户店铺');
    expect(presentation.actionPath).toBe('/merchant-store/9');
  });

  it('marks inactive products as not purchasable', () => {
    const presentation = getSaleStatusPresentation(0);

    expect(presentation.label).toBe('商品已下架');
    expect(presentation.description).toContain('暂不可购买');
    expect(presentation.isPurchasable).toBe(false);
  });

  it('prefers backend product detail contract fields when available', () => {
    const presentation = getProductDetailPresentation({
      stock: 2,
      unit: '份',
      status: 1,
      fulfillmentMode: 'EXPRESS_DELIVERY',
      productType: 'PHYSICAL',
      inventoryLabel: '后端库存提示',
      inventoryDescription: '后端库存说明',
      fulfillmentLabel: '后端履约方式',
      fulfillmentDescription: '后端履约说明',
      afterSalesNote: '后端售后说明',
      purchaseLimitNote: '后端购买限制',
      deliveryAccessDescription: '后端查看位置',
      deliveryAccessActionLabel: '后端动作',
      purchasable: false,
    });

    expect(presentation.inventory.label).toBe('后端库存提示');
    expect(presentation.inventory.description).toBe('后端库存说明');
    expect(presentation.fulfillment.label).toBe('后端履约方式');
    expect(presentation.fulfillment.description).toBe('后端履约说明');
    expect(presentation.afterSalesNote).toBe('后端售后说明');
    expect(presentation.purchaseLimitNote).toBe('后端购买限制');
    expect(presentation.deliveryAccess.description).toBe('后端查看位置');
    expect(presentation.deliveryAccess.actionLabel).toBe('后端动作');
    expect(presentation.saleStatus.isPurchasable).toBe(false);
    expect(presentation.inventory.isOutOfStock).toBe(false);
  });
});
