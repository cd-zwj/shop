import { describe, expect, it } from 'vitest';
import type { SalesOrderDetail } from '../types/order';
import {
  buildMerchantWorkItems,
  getOrderLifecyclePresentation,
  normalizeSalesOrderDetail,
} from './orderLifecycle';

describe('orderLifecycle', () => {
  it('normalizes flat backend order detail into the nested frontend contract', () => {
    const detail = normalizeSalesOrderDetail({
      id: 1,
      tenantId: 2,
      platformUserId: 3,
      orderNo: 'SO001',
      orderStatus: 'PAID',
      payStatus: 'SUCCESS',
      totalAmount: 1200,
      payableAmount: 1000,
      items: [{
        id: 9,
        orderId: 1,
        orderNo: 'SO001',
        tenantId: 2,
        productId: 8,
        productName: '课程',
        price: 1000,
        quantity: 1,
        subtotal: 1000,
      }],
      paymentBillNo: 'PB001',
    });

    expect(detail.order.orderNo).toBe('SO001');
    expect(detail.order.tenantId).toBe(2);
    expect(detail.items).toHaveLength(1);
    expect(detail.paymentBillNo).toBe('PB001');
  });

  it('keeps already nested order detail unchanged', () => {
    const nested: SalesOrderDetail = {
      order: {
        id: 1,
        tenantId: 2,
        platformUserId: 3,
        orderNo: 'SO002',
        orderStatus: 'CREATED',
        payStatus: 'WAIT_PAY',
        totalAmount: 500,
      },
      items: [],
      paymentBillNo: null,
    };

    expect(normalizeSalesOrderDetail(nested)).toEqual(nested);
  });

  it('describes user-visible lifecycle states and next actions', () => {
    expect(getOrderLifecyclePresentation({ orderStatus: 'CREATED', payStatus: 'WAIT_PAY' }).label).toBe('待支付');
    expect(getOrderLifecyclePresentation({ orderStatus: 'PAID', payStatus: 'SUCCESS' }).nextActions.map((item) => item.key)).toContain('refund');
    expect(getOrderLifecyclePresentation({ orderStatus: 'CANCELLED', payStatus: 'CLOSED' }).nextActions.map((item) => item.key)).toContain('repurchase');
    expect(getOrderLifecyclePresentation({ orderStatus: 'CREATED', payStatus: 'FAILED' }).label).toBe('已关闭');
  });

  it('builds actionable merchant work items from orders and stock', () => {
    const items = buildMerchantWorkItems({
      orders: [
        { orderNo: 'SO1', orderStatus: 'CREATED', payStatus: 'WAIT_PAY', totalAmount: 100 },
        { orderNo: 'SO2', orderStatus: 'PAID', payStatus: 'SUCCESS', totalAmount: 200 },
      ],
      products: [
        { stock: 3, status: 'active' },
        { stock: 20, status: 'active' },
      ],
    });

    expect(items.map((item) => item.count)).toEqual([1, 1, 1]);
    expect(items[1].path).toBe('/merchant/orders');
  });
});
