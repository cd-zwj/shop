import React, { act } from 'react';
import { createRoot, type Root } from 'react-dom/client';
import { MemoryRouter, Route, Routes } from 'react-router-dom';
import { afterEach, describe, expect, it, vi } from 'vitest';
import MerchantOrders from './MerchantOrders';
import { merchantOrderService } from '../../services/modules/merchantOrder';
import type { MerchantOrder } from '../../types/merchant';

vi.mock('../../context/AuthContext', () => ({
  useAuth: () => ({
    merchantSession: {
      tenantId: 9,
      tenantName: '测试店铺',
      employeeRole: 'OWNER',
    },
  }),
}));

vi.mock('../../services/modules/merchantOrder', () => ({
  merchantOrderService: {
    listOrders: vi.fn(),
  },
}));

const mockedMerchantOrderService = vi.mocked(merchantOrderService);

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

async function renderMerchantOrders() {
  container = document.createElement('div');
  document.body.appendChild(container);
  root = createRoot(container);

  await act(async () => {
    root?.render(
      React.createElement(
        MemoryRouter,
        { initialEntries: ['/merchant/orders'] },
        React.createElement(
          Routes,
          null,
          React.createElement(Route, { path: '/merchant/orders', element: React.createElement(MerchantOrders) }),
        ),
      ),
    );
  });

  await flushAsyncWork();
  return container;
}

describe('MerchantOrders', () => {
  it('shows a retryable error state when merchant orders fail to load', async () => {
    mockedMerchantOrderService.listOrders
      .mockRejectedValueOnce(new Error('订单接口不可用'))
      .mockResolvedValueOnce({
        records: [buildOrder({
          orderNo: 'MO202607060001',
          subject: '测试履约订单',
          payStatus: 'SUCCESS',
        })],
        total: 1,
        page: 1,
        size: 100,
        pages: 1,
      });

    const element = await renderMerchantOrders();

    expect(element.textContent).toContain('订单接口不可用');
    expect(element.textContent).toContain('重试');

    const retryButton = Array.from(element.querySelectorAll('button'))
      .find((button) => button.textContent?.includes('重试'));
    expect(retryButton).toBeTruthy();

    await act(async () => {
      retryButton?.dispatchEvent(new MouseEvent('click', { bubbles: true }));
    });
    await flushAsyncWork();

    expect(element.textContent).toContain('MO202607060001');
    expect(element.textContent).toContain('测试履约订单');
    expect(Array.from(element.querySelectorAll('button'))
      .some((button) => button.textContent?.includes('重试'))).toBe(false);
  });
});

function buildOrder(overrides: Partial<MerchantOrder>): MerchantOrder {
  return {
    id: 1,
    orderNo: 'MO202607060000',
    tenantId: 9,
    platformUserId: 88,
    orderStatus: 'PAID',
    payStatus: 'SUCCESS',
    totalAmount: 128,
    subject: '订单',
    source: 'APP',
    createTime: '2026-07-06T10:00:00',
    updateTime: null,
    ...overrides,
  };
}
