import { describe, expect, it } from 'vitest';
import { getPurchaseDeliveryPresentation } from './purchaseDelivery';
import type { PurchaseRecord } from '../services/modules/appPurchases';

const baseRecord: PurchaseRecord = {
  id: 1,
  orderId: 10,
  orderNo: 'SO202607050001',
  orderItemId: 20,
  productId: 30,
  productName: '课程资料包',
  productType: 'VIRTUAL',
  status: 'DELIVERED',
  payload: '{"contentUrl":"https://example.com/file.pdf","accountInfo":"student-01"}',
  createTime: '2026-07-05T10:00:00',
};

describe('purchaseDelivery', () => {
  it('uses product name as the primary title', () => {
    const presentation = getPurchaseDeliveryPresentation(baseRecord);

    expect(presentation.title).toBe('课程资料包');
    expect(presentation.subtitle).toContain('虚拟内容');
  });

  it('shows reopen action for delivered virtual content', () => {
    const presentation = getPurchaseDeliveryPresentation(baseRecord);

    expect(presentation.primaryAction?.label).toBe('重新查看内容');
    expect(presentation.primaryAction?.value).toBe('https://example.com/file.pdf');
  });

  it('shows copy action for delivered card keys', () => {
    const presentation = getPurchaseDeliveryPresentation({
      ...baseRecord,
      productName: '会员兑换码',
      productType: 'CARD_KEY',
      payload: '{"code":"VIP-2026-0001"}',
    });

    expect(presentation.primaryAction?.label).toBe('复制兑换码');
    expect(presentation.primaryAction?.value).toBe('VIP-2026-0001');
  });

  it('shows contact action when revoke failed', () => {
    const presentation = getPurchaseDeliveryPresentation({
      ...baseRecord,
      status: 'REVOKE_FAILED',
      failReason: '卡密作废接口超时',
    });

    expect(presentation.guidance).toContain('卡密作废接口超时');
    expect(presentation.primaryAction?.label).toBe('联系商户');
  });
});
