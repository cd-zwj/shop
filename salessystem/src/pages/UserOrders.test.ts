import React, { act } from 'react';
import { createRoot, type Root } from 'react-dom/client';
import { MemoryRouter, Route, Routes } from 'react-router-dom';
import { afterEach, describe, expect, it, vi } from 'vitest';
import UserOrders from './UserOrders';
import { appOrderService } from '../services/modules/appOrder';
import { appRefundService } from '../services/modules/appRefund';
import type { SalesOrder, SalesOrderDetail } from '../types/order';

vi.mock('motion/react', () => ({
  motion: new Proxy({}, {
    get: (_target, tag: string) => tag,
  }),
}));

vi.mock('../services/modules/appOrder', () => ({
  appOrderService: {
    listOrders: vi.fn(),
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

async function renderUserOrders() {
  container = document.createElement('div');
  document.body.appendChild(container);
  root = createRoot(container);

  await act(async () => {
    root?.render(
      React.createElement(
        MemoryRouter,
        { initialEntries: ['/orders'] },
        React.createElement(
          Routes,
          null,
          React.createElement(Route, { path: '/orders', element: React.createElement(UserOrders) }),
          React.createElement(Route, { path: '/order/:orderNo', element: React.createElement('div', null, '订单详情路由') }),
        ),
      ),
    );
  });

  await flushAsyncWork();
  return container;
}

describe('UserOrders', () => {
  it('shows a retryable error state when the order list fails to load', async () => {
    const order = buildOrder({
      id: 10,
      orderNo: 'SO202607070001',
      subject: '本地测试订单',
      totalAmount: 128,
    });
    const detail = buildOrderDetail(order);

    mockedOrderService.listOrders
      .mockRejectedValueOnce(new Error('订单服务暂不可用'))
      .mockResolvedValue({
        records: [order],
        total: 1,
        page: 1,
        current: 1,
        size: 20,
        pages: 1,
      });
    mockedOrderService.getOrder.mockResolvedValue(detail);
    mockedRefundService.listRefunds.mockResolvedValue({
      records: [],
      total: 0,
      page: 1,
      current: 1,
      size: 100,
      pages: 0,
    });

    const element = await renderUserOrders();

    expect(element.textContent).toContain('订单列表加载失败');
    expect(element.textContent).toContain('订单服务暂不可用');
    expect(mockedOrderService.listOrders).toHaveBeenCalledTimes(1);

    const retryButton = Array.from(element.querySelectorAll('button'))
      .find((button) => button.textContent?.includes('重试'));
    expect(retryButton).toBeTruthy();

    await act(async () => {
      retryButton?.dispatchEvent(new MouseEvent('click', { bubbles: true }));
    });
    await flushAsyncWork();

    expect(mockedOrderService.listOrders).toHaveBeenCalledTimes(2);
    expect(element.textContent).toContain('本地测试订单');
    expect(element.textContent).not.toContain('订单列表加载失败');
  });
});

function buildOrder(overrides: Partial<SalesOrder> = {}): SalesOrder {
  return {
    id: 1,
    orderNo: 'SO202607070000',
    tenantId: 3,
    platformUserId: 5,
    orderStatus: 'PAID',
    payStatus: 'PAID',
    totalAmount: 99,
    externalPayAmount: 0,
    subject: '测试订单',
    createTime: '2026-07-07T10:00:00',
    ...overrides,
  };
}

function buildOrderDetail(order: SalesOrder): SalesOrderDetail {
  return {
    order,
    items: [],
    paymentBillNo: null,
    paymentBillStatus: null,
    paymentBillStatusRemark: null,
    paymentBillExpireTime: null,
  };
}
