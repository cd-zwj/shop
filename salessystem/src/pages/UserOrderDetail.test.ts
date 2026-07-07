import React, { act } from 'react';
import { createRoot, type Root } from 'react-dom/client';
import { MemoryRouter, Route, Routes } from 'react-router-dom';
import { afterEach, describe, expect, it, vi } from 'vitest';
import UserOrderDetail from './UserOrderDetail';
import { appOrderService } from '../services/modules/appOrder';
import { appRefundService } from '../services/modules/appRefund';
import type { SalesOrder, SalesOrderDetail } from '../types/order';

vi.mock('../context/CartContext', () => ({
  useCart: () => ({
    addCartItems: vi.fn(),
  }),
}));

vi.mock('../services/modules/appOrder', () => ({
  appOrderService: {
    getOrder: vi.fn(),
    repayOrder: vi.fn(),
    cancelOrder: vi.fn(),
  },
}));

vi.mock('../services/modules/appRefund', () => ({
  appRefundService: {
    listRefunds: vi.fn(),
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

async function renderUserOrderDetail() {
  container = document.createElement('div');
  document.body.appendChild(container);
  root = createRoot(container);

  await act(async () => {
    root?.render(
      React.createElement(
        MemoryRouter,
        { initialEntries: ['/order/SO202607080001'] },
        React.createElement(
          Routes,
          null,
          React.createElement(Route, { path: '/order/:id', element: React.createElement(UserOrderDetail) }),
        ),
      ),
    );
  });

  await flushAsyncWork();
  return container;
}

describe('UserOrderDetail', () => {
  it('lets users retry a failed detail load and then shows payment failure actions', async () => {
    mockedOrderService.getOrder
      .mockRejectedValueOnce(new Error('订单详情接口暂不可用'))
      .mockResolvedValueOnce(buildOrderDetail(buildOrder({
        orderStatus: 'CREATED',
        payStatus: 'FAILED',
      })));
    mockedRefundService.listRefunds.mockResolvedValue({
      records: [],
      total: 0,
      page: 1,
      current: 1,
      size: 100,
      pages: 0,
    });

    const element = await renderUserOrderDetail();

    expect(element.textContent).toContain('订单详情加载失败');
    expect(element.textContent).toContain('订单详情接口暂不可用');

    const retryButton = Array.from(element.querySelectorAll('button'))
      .find((button) => button.textContent?.includes('重试'));
    expect(retryButton).toBeTruthy();

    await act(async () => {
      retryButton?.dispatchEvent(new MouseEvent('click', { bubbles: true }));
    });
    await flushAsyncWork();

    expect(mockedOrderService.getOrder).toHaveBeenCalledTimes(2);
    expect(element.textContent).toContain('支付失败');
    expect(element.textContent).toContain('渠道返回：余额不足');
    expect(element.textContent).toContain('继续支付 / 查看支付状态');
    expect(element.textContent).not.toContain('订单详情加载失败');
  });
});

function buildOrder(overrides: Partial<SalesOrder> = {}): SalesOrder {
  return {
    id: 1,
    orderNo: 'SO202607080001',
    tenantId: 9,
    platformUserId: 3,
    orderStatus: 'PAID',
    payStatus: 'SUCCESS',
    totalAmount: 9900,
    payableAmount: 9900,
    externalPayAmount: 9900,
    subject: '本地订单',
    createTime: '2026-07-08T10:00:00',
    ...overrides,
  };
}

function buildOrderDetail(order: SalesOrder): SalesOrderDetail {
  return {
    order,
    items: [],
    paymentBillNo: 'PB202607080001',
    paymentBillStatus: 'FAILED',
    paymentBillStatusRemark: '渠道返回：余额不足',
    paymentBillExpireTime: null,
  };
}
