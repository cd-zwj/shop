import { describe, expect, it } from 'vitest';
import type { SalesOrderDetail } from '../types/order';
import {
  buildMerchantWorkItems,
  getOrderLifecyclePresentation,
  getOrderProgressPresentation,
  normalizeSalesOrderDetail,
  prioritizeMerchantWorkItems,
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
    expect(detail.paymentBillStatus).toBeNull();
  });

  it('keeps flat payment bill status context on order detail top level', () => {
    const detail = normalizeSalesOrderDetail({
      id: 1,
      tenantId: 2,
      platformUserId: 3,
      orderNo: 'SO003',
      orderStatus: 'CREATED',
      payStatus: 'FAILED',
      totalAmount: 1200,
      items: [],
      paymentBillNo: 'PB003',
      paymentBillStatus: 'FAILED',
      paymentBillStatusRemark: '渠道返回：余额不足',
      paymentBillExpireTime: '2026-07-05 10:30:00',
    });

    expect(detail.paymentBillNo).toBe('PB003');
    expect(detail.paymentBillStatus).toBe('FAILED');
    expect(detail.paymentBillStatusRemark).toBe('渠道返回：余额不足');
    expect(detail.paymentBillExpireTime).toBe('2026-07-05 10:30:00');
    expect('paymentBillStatus' in detail.order).toBe(false);
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
    expect(getOrderLifecyclePresentation({ orderStatus: 'CREATED', payStatus: 'PAYING' }).label).toBe('支付中');
    expect(getOrderLifecyclePresentation({ orderStatus: 'PAID', payStatus: 'SUCCESS' }).nextActions.map((item) => item.key)).toContain('refund');
    expect(getOrderLifecyclePresentation({ orderStatus: 'CANCELLED', payStatus: 'CLOSED' }).nextActions.map((item) => item.key)).toContain('repurchase');
    expect(getOrderLifecyclePresentation({ orderStatus: 'CREATED', payStatus: 'FAILED' }).label).toBe('支付失败');
    expect(getOrderLifecyclePresentation({ orderStatus: 'CREATED', payStatus: 'FAILED' }, {
      paymentBillStatusRemark: '渠道返回：余额不足',
    }).failureReason).toContain('余额不足');
  });

  it('prefers backend order presentation when the API provides it', () => {
    const presentation = getOrderLifecyclePresentation({
      orderStatus: 'CREATED',
      payStatus: 'WAIT_PAY',
      statusLabel: '支付失败',
      statusDescription: '失败原因：渠道返回余额不足',
      nextStep: '下一步：重新发起支付。',
      failureReason: '渠道返回余额不足',
      availableActions: ['PAY', 'CONTACT_MERCHANT'],
    });

    expect(presentation.label).toBe('支付失败');
    expect(presentation.failureReason).toBe('渠道返回余额不足');
    expect(presentation.nextActions.map((action) => action.key)).toEqual(['pay', 'contact']);
  });

  it('uses delivery records to distinguish paid fulfillment states', () => {
    const baseOrder = { orderStatus: 'PAID', payStatus: 'SUCCESS' };

    expect(getOrderLifecyclePresentation(baseOrder).label).toBe('已支付');
    expect(getOrderLifecyclePresentation(baseOrder, {
      items: [{ deliveryStatus: 'PENDING' }],
    }).label).toBe('待发货');
    expect(getOrderLifecyclePresentation(baseOrder, {
      items: [{ deliveryStatus: 'DELIVERING' }],
    }).label).toBe('发货中');
    expect(getOrderLifecyclePresentation(baseOrder, {
      items: [{ deliveryStatus: 'DELIVERED' }],
    }).label).toBe('已发货');
    expect(getOrderLifecyclePresentation(baseOrder, {
      items: [{ deliveryStatus: 'CONFIRMED' }],
    }).label).toBe('已完成');
  });

  it('uses refund status as the highest priority lifecycle signal', () => {
    const baseOrder = { orderStatus: 'PAID', payStatus: 'SUCCESS' };

    expect(getOrderLifecyclePresentation(baseOrder, {
      refunds: [{ refundStatus: 'PROCESSING', refundSuggestion: '等待内部退款单完成' }],
    }).label).toBe('退款中');
    expect(getOrderLifecyclePresentation(baseOrder, {
      refunds: [{ refundStatus: 'COMPLETED' }],
    }).label).toBe('已退款');
    expect(getOrderLifecyclePresentation(baseOrder, {
      refunds: [{ refundStatus: 'FAILED', rejectReason: '交付撤销失败' }],
    }).failureReason).toContain('交付撤销失败');
  });

  it('calculates order progress from the visible lifecycle state', () => {
    const pending = getOrderProgressPresentation(
      { orderStatus: 'CREATED', payStatus: 'WAIT_PAY' },
      getOrderLifecyclePresentation({ orderStatus: 'CREATED', payStatus: 'WAIT_PAY' }),
    );
    expect(pending.steps.map((step) => step.active)).toEqual([true, false, false, false]);
    expect(pending.progressPercent).toBe(0);

    const paid = getOrderProgressPresentation(
      { orderStatus: 'PAID', payStatus: 'SUCCESS' },
      getOrderLifecyclePresentation({ orderStatus: 'PAID', payStatus: 'SUCCESS' }),
    );
    expect(paid.steps.map((step) => step.active)).toEqual([true, true, false, false]);
    expect(paid.progressPercent).toBeCloseTo(33.33, 1);

    const fulfilled = getOrderProgressPresentation(
      { orderStatus: 'PAID', payStatus: 'SUCCESS' },
      getOrderLifecyclePresentation(
        { orderStatus: 'PAID', payStatus: 'SUCCESS' },
        { items: [{ deliveryStatus: 'CONFIRMED' }] },
      ),
    );
    expect(fulfilled.steps.map((step) => step.active)).toEqual([true, true, true, true]);
    expect(fulfilled.progressPercent).toBe(100);

    const failed = getOrderProgressPresentation(
      { orderStatus: 'CREATED', payStatus: 'FAILED' },
      getOrderLifecyclePresentation({ orderStatus: 'CREATED', payStatus: 'FAILED' }),
    );
    expect(failed.steps.map((step) => step.active)).toEqual([true, false, false, true]);
    expect(failed.progressPercent).toBe(100);
  });

  it('keeps refund lifecycle visible when an order already has delivery records', () => {
    const presentation = getOrderLifecyclePresentation(
      { orderStatus: 'PAID', payStatus: 'SUCCESS' },
      {
        items: [{ deliveryStatus: 'CONFIRMED' }],
        refunds: [{ refundStatus: 'REJECTED', rejectReason: '超过售后期' }],
      },
    );

    expect(presentation.label).toBe('退款驳回');
    expect(presentation.failureReason).toBe('超过售后期');
    expect(presentation.nextActions.map((action) => action.key)).toEqual(['contact', 'refund']);
  });

  it('builds actionable merchant work items from orders and stock', () => {
    const items = buildMerchantWorkItems({
      orders: [
        { orderNo: 'SO1', orderStatus: 'CREATED', payStatus: 'WAIT_PAY', totalAmount: 100 },
        { orderNo: 'SO2', orderStatus: 'PAID', payStatus: 'SUCCESS', totalAmount: 200 },
        { orderNo: 'SO3', orderStatus: 'CREATED', payStatus: 'FAILED', totalAmount: 300 },
      ],
      products: [
        { stock: 3, status: 'active' },
        { stock: 20, status: 'active' },
      ],
      refunds: [
        { refundNo: 'R1', refundStatus: 'PENDING' },
        { refundNo: 'R2', refundStatus: 'FAILED' },
        { refundNo: 'R3', refundStatus: 'COMPLETED' },
      ],
    });

    expect(items.map((item) => item.count)).toEqual([1, 1, 1, 1, 1, 1]);
    expect(items[0].path).toBe('/merchant/orders?tab=pending');
    expect(items[1].path).toBe('/merchant/orders?tab=shipping');
    expect(items[2].path).toBe('/merchant/orders?tab=abnormal');
    expect(items[3].path).toBe('/merchant/refunds?status=PENDING');
    expect(items[4].path).toBe('/merchant/refunds?status=FAILED');
  });

  it('prioritizes actionable merchant work items before empty informational items', () => {
    const items = buildMerchantWorkItems({
      orders: [
        { orderNo: 'SO2', orderStatus: 'PAID', payStatus: 'SUCCESS', totalAmount: 200 },
        { orderNo: 'SO3', orderStatus: 'CREATED', payStatus: 'FAILED', totalAmount: 300 },
      ],
      products: [
        { stock: 20, status: 'active' },
      ],
      refunds: [
        { refundNo: 'R1', refundStatus: 'PENDING' },
      ],
    });

    const prioritized = prioritizeMerchantWorkItems(items);

    expect(prioritized.slice(0, 3).map((item) => item.key)).toEqual([
      'abnormalOrder',
      'refund',
      'fulfillment',
    ]);
    expect(prioritized.at(-1)?.count).toBe(0);
  });
});
