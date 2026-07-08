import React, { act } from 'react';
import { createRoot, type Root } from 'react-dom/client';
import { MemoryRouter, Route, Routes } from 'react-router-dom';
import { afterEach, describe, expect, it, vi } from 'vitest';
import ApplyRefund from './ApplyRefund';
import { appOrderService } from '../services/modules/appOrder';
import { appRefundService } from '../services/modules/appRefund';
import { ApiError } from '../types/api';

const mockShowToast = vi.fn();

vi.mock('../context/ToastContext', () => ({
  useToast: () => ({
    showToast: mockShowToast,
  }),
}));

vi.mock('../services/modules/appOrder', () => ({
  appOrderService: {
    getOrder: vi.fn(),
  },
}));

vi.mock('../services/modules/appRefund', () => ({
  appRefundService: {
    applyRefund: vi.fn(),
    listRefunds: vi.fn(),
    cancelRefund: vi.fn(),
  },
}));

const mockedOrderService = vi.mocked(appOrderService);
const mockedRefundService = vi.mocked(appRefundService);

let root: Root | null = null;
let container: HTMLDivElement | null = null;

afterEach(() => {
  if (root) {
    act(() => root?.unmount());
  }
  container?.remove();
  root = null;
  container = null;
  vi.clearAllMocks();
});

async function flushAsyncWork() {
  await act(async () => {
    await Promise.resolve();
    await Promise.resolve();
    await Promise.resolve();
  });
}

async function renderApplyRefund() {
  container = document.createElement('div');
  document.body.appendChild(container);
  root = createRoot(container);

  await act(async () => {
    root?.render(
      React.createElement(
        MemoryRouter,
        { initialEntries: ['/orders/SO001/refund'] },
        React.createElement(
          Routes,
          null,
          React.createElement(Route, { path: '/orders/:orderNo/refund', element: React.createElement(ApplyRefund) }),
          React.createElement(Route, { path: '/merchant-store/:tenantId', element: React.createElement('div', null, '商户主页') }),
        ),
      ),
    );
  });

  await flushAsyncWork();
  return container;
}

describe('ApplyRefund', () => {
  it('shows a retryable load error and reloads refund data', async () => {
    mockedOrderService.getOrder
      .mockRejectedValueOnce(new ApiError('订单服务暂时不可用', 503))
      .mockResolvedValueOnce({
        order: {
          id: 1,
          tenantId: 9,
          platformUserId: 3,
          orderNo: 'SO001',
          orderStatus: 'PAID',
          payStatus: 'SUCCESS',
          totalAmount: 128,
          payableAmount: 128,
          createTime: '2026-07-07T10:00:00',
        },
        items: [{
          id: 10,
          orderId: 1,
          orderNo: 'SO001',
          tenantId: 9,
          productId: 20,
          productName: '测试商品',
          price: 128,
          quantity: 1,
          subtotal: 128,
        }],
        paymentBillNo: null,
      });
    mockedRefundService.listRefunds.mockResolvedValue({
      records: [],
      total: 0,
      page: 1,
      size: 10,
      pages: 0,
    });

    const element = await renderApplyRefund();

    expect(element.textContent).toContain('数据加载失败');
    expect(element.textContent).toContain('订单服务暂时不可用');
    const retryButton = Array.from(element.querySelectorAll('button')).find((button) => button.textContent?.includes('重试加载'));
    expect(retryButton).toBeTruthy();

    await act(async () => {
      retryButton?.dispatchEvent(new MouseEvent('click', { bubbles: true }));
    });
    await flushAsyncWork();

    expect(mockedOrderService.getOrder).toHaveBeenCalledTimes(2);
    expect(mockedRefundService.listRefunds).toHaveBeenCalledTimes(1);
    expect(element.textContent).toContain('对应订单信息');
    expect(element.textContent).toContain('新建退款申请');
  });

  it('shows failed refund reason with merchant contact and reapply actions', async () => {
    mockedOrderService.getOrder.mockResolvedValue({
      order: {
        id: 1,
        tenantId: 9,
        platformUserId: 3,
        orderNo: 'SO001',
        orderStatus: 'PAID',
        payStatus: 'SUCCESS',
        totalAmount: 128,
        payableAmount: 128,
        createTime: '2026-07-07T10:00:00',
      },
      items: [{
        id: 10,
        orderId: 1,
        orderNo: 'SO001',
        tenantId: 9,
        productId: 20,
        productName: '测试商品',
        price: 128,
        quantity: 1,
        subtotal: 128,
      }],
      paymentBillNo: null,
    });
    mockedRefundService.listRefunds.mockResolvedValue({
      records: [{
        id: 88,
        refundNo: 'RF001',
        orderNo: 'SO001',
        orderItemId: null,
        refundType: 'REFUND_ONLY',
        refundStatus: 'FAILED',
        refundAmount: 128,
        deliveryStatus: null,
        refundableAmount: 128,
        quickRefundSuggested: false,
        refundSuggestion: null,
        reason: '商品质量问题',
        description: null,
        rejectReason: '内部退款单处理失败',
        auditTime: null,
        completeTime: null,
        createTime: '2026-07-07T10:10:00',
      }],
      total: 1,
      page: 1,
      size: 10,
      pages: 1,
    });

    const element = await renderApplyRefund();

    expect(element.textContent).toContain('失败原因：内部退款单处理失败');
    expect(element.textContent).toContain('建议联系商户处理');
    expect(element.textContent).toContain('联系商户');
    expect(element.textContent).toContain('重新申请售后');
    expect(element.textContent).toContain('新建退款申请');
  });
});
