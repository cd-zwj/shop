import { describe, expect, it } from 'vitest';
import { getOrderItemFulfillmentPresentation } from './orderFulfillment';
import type { SalesOrderItem } from '../types/order';

const baseItem: SalesOrderItem = {
  id: 1,
  orderId: 10,
  orderNo: 'SO20260706001',
  tenantId: 20,
  productId: 30,
  productName: '测试商品',
  price: 100,
  quantity: 1,
  subtotal: 100,
  productType: 'PHYSICAL',
  deliveryStatus: 'PENDING',
};

describe('orderFulfillment', () => {
  it('explains pending physical shipment', () => {
    expect(getOrderItemFulfillmentPresentation(baseItem)).toEqual({
      label: '待发货',
      description: '商户尚未填写物流信息，发货后可在订单和我的已购中查看。',
      tone: 'warning',
      actionLabel: undefined,
      actionPath: undefined,
    });
  });

  it('links delivered physical item to purchase records', () => {
    expect(getOrderItemFulfillmentPresentation({
      ...baseItem,
      deliveryStatus: 'DELIVERED',
    })).toEqual({
      label: '已发货',
      description: '物流信息已生成，可前往我的已购查看物流单号并确认收货。',
      tone: 'success',
      actionLabel: '查看履约记录',
      actionPath: '/my-purchases?orderNo=SO20260706001',
    });
  });

  it('explains delivered virtual content can be reopened', () => {
    expect(getOrderItemFulfillmentPresentation({
      ...baseItem,
      productType: 'VIRTUAL',
      deliveryStatus: 'DELIVERED',
    })).toEqual({
      label: '已交付',
      description: '虚拟内容已发放，可在我的已购中重新查看文件、链接或账号信息。',
      tone: 'success',
      actionLabel: '查看交付内容',
      actionPath: '/my-purchases?orderNo=SO20260706001',
    });
  });

  it('explains failed card key delivery and contact next step', () => {
    expect(getOrderItemFulfillmentPresentation({
      ...baseItem,
      productType: 'CARD_KEY',
      deliveryStatus: 'FAILED',
    })).toMatchObject({
      label: '交付失败',
      description: '兑换码交付失败，请联系商户处理或稍后刷新履约记录。',
      tone: 'danger',
      actionLabel: '查看履约记录',
    });
  });
});
