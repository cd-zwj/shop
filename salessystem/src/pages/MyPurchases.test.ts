import React, { act } from 'react';
import { createRoot, type Root } from 'react-dom/client';
import { MemoryRouter, Route, Routes } from 'react-router-dom';
import { afterEach, describe, expect, it, vi } from 'vitest';
import MyPurchases from './MyPurchases';
import { ToastProvider } from '../context/ToastContext';
import { appPurchasesService, type PurchaseRecord } from '../services/modules/appPurchases';

vi.mock('../services/modules/appPurchases', () => ({
  appPurchasesService: {
    list: vi.fn(),
    detail: vi.fn(),
    confirm: vi.fn(),
  },
}));

const mockedPurchasesService = vi.mocked(appPurchasesService);

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

async function renderMyPurchases(initialEntry = '/my-purchases') {
  container = document.createElement('div');
  document.body.appendChild(container);
  root = createRoot(container);

  await act(async () => {
    root?.render(
      React.createElement(
        MemoryRouter,
        { initialEntries: [initialEntry] },
        React.createElement(
          ToastProvider,
          null,
          React.createElement(
            Routes,
            null,
            React.createElement(Route, { path: '/my-purchases', element: React.createElement(MyPurchases) }),
          ),
        ),
      ),
    );
  });

  await flushAsyncWork();
  return container;
}

describe('MyPurchases', () => {
  it('shows a retryable error state when purchase delivery records fail to load', async () => {
    mockedPurchasesService.list
      .mockRejectedValueOnce(new Error('交付记录服务不可用'))
      .mockResolvedValueOnce({
        records: [buildPurchase({
          id: 17,
          productName: '数字资料包',
          productType: 'VIRTUAL',
          status: 'DELIVERED',
          payload: JSON.stringify({ contentUrl: 'http://localhost/assets/course.zip' }),
        })],
        total: 1,
        page: 1,
        size: 50,
        pages: 1,
      });

    const element = await renderMyPurchases();

    expect(element.textContent).toContain('交付记录服务不可用');
    expect(element.textContent).toContain('重试');
    expect(element.textContent).not.toContain('还没有已购商品');

    const retryButton = Array.from(element.querySelectorAll('button'))
      .find((button) => button.textContent?.includes('重试'));
    expect(retryButton).toBeTruthy();

    await act(async () => {
      retryButton?.dispatchEvent(new MouseEvent('click', { bubbles: true }));
    });
    await flushAsyncWork();

    expect(element.textContent).toContain('数字资料包');
    expect(element.textContent).toContain('重新查看内容');
    expect(Array.from(element.querySelectorAll('button'))
      .some((button) => button.textContent?.includes('重试'))).toBe(false);
  });

  it('loads purchase records by order number when opened from order detail', async () => {
    mockedPurchasesService.list.mockResolvedValue({
      records: [buildPurchase({
        orderNo: 'SO202607060001',
        productName: '会员兑换码',
        productType: 'CARD_KEY',
        status: 'DELIVERED',
        payload: JSON.stringify({ code: 'VIP-2026-0001' }),
      })],
      total: 1,
      page: 1,
      size: 50,
      pages: 1,
    });

    const element = await renderMyPurchases('/my-purchases?orderNo=SO202607060001');

    expect(mockedPurchasesService.list).toHaveBeenCalledWith(undefined, 1, 50, 'SO202607060001');
    expect(element.textContent).toContain('正在查看订单 SO202607060001 的履约记录');
    expect(element.textContent).toContain('会员兑换码');
    expect(element.textContent).toContain('复制兑换码');
  });
});

function buildPurchase(overrides: Partial<PurchaseRecord>): PurchaseRecord {
  return {
    id: 1,
    tenantId: 9,
    orderId: 2,
    orderNo: 'SO202607060001',
    orderItemId: 3,
    productId: 4,
    productName: '已购商品',
    productType: 'CARD_KEY',
    status: 'DELIVERED',
    payload: JSON.stringify({ code: 'ABC-123' }),
    failReason: null,
    deliveredTime: '2026-07-06T10:00:00',
    confirmedTime: null,
    expireTime: null,
    createTime: '2026-07-06T09:00:00',
    ...overrides,
  };
}
