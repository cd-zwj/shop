import React, { act } from 'react';
import { createRoot, type Root } from 'react-dom/client';
import { MemoryRouter, Route, Routes } from 'react-router-dom';
import { afterEach, describe, expect, it, vi } from 'vitest';
import MerchantWalletDetail from './MerchantWalletDetail';
import { appCatalogService } from '../services/modules/appCatalog';
import { appWalletService } from '../services/modules/appWallet';

vi.mock('motion/react', () => ({
  motion: new Proxy({}, {
    get: (_target, tag: string) => tag,
  }),
}));

vi.mock('../services/modules/appCatalog', () => ({
  appCatalogService: {
    getTenant: vi.fn(),
  },
}));

vi.mock('../services/modules/appWallet', () => ({
  appWalletService: {
    getMerchantWallet: vi.fn(),
    getMerchantWalletLogs: vi.fn(),
  },
}));

const mockedCatalogService = vi.mocked(appCatalogService);
const mockedWalletService = vi.mocked(appWalletService);

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

async function renderMerchantWalletDetail() {
  container = document.createElement('div');
  document.body.appendChild(container);
  root = createRoot(container);

  await act(async () => {
    root?.render(
      React.createElement(
        MemoryRouter,
        { initialEntries: ['/wallet/tenants/9'] },
        React.createElement(
          Routes,
          null,
          React.createElement(Route, { path: '/wallet/tenants/:tenantId', element: React.createElement(MerchantWalletDetail) }),
        ),
      ),
    );
  });

  return container;
}

describe('MerchantWalletDetail', () => {
  it('shows merchant wallet balance and traceable wallet logs', async () => {
    mockedCatalogService.getTenant.mockResolvedValue({
      id: 9,
      name: '本地测试店',
      status: 1,
    });
    mockedWalletService.getMerchantWallet.mockResolvedValue({
      walletType: 'MERCHANT',
      tenantId: 9,
      availableAmount: 168,
      frozenAmount: 12,
      totalRecharge: 300,
      totalConsume: 120,
    });
    mockedWalletService.getMerchantWalletLogs.mockResolvedValue({
      records: [{
        walletType: 'MERCHANT',
        tenantId: 9,
        bizType: 'SALES_ORDER',
        bizNo: 'SO202607080001',
        changeAmount: -32,
        balanceBefore: 200,
        balanceAfter: 168,
        remark: '订单消费扣减',
        createTime: '2026-07-08T10:00:00',
      }],
      total: 1,
      page: 1,
      size: 10,
      pages: 1,
    });

    const element = await renderMerchantWalletDetail();

    expect(mockedCatalogService.getTenant).toHaveBeenCalledWith(9);
    expect(mockedWalletService.getMerchantWallet).toHaveBeenCalledWith(9);
    expect(mockedWalletService.getMerchantWalletLogs).toHaveBeenCalledWith(9, 1, 10);
    expect(element.textContent).toContain('本地测试店');
    expect(element.textContent).toContain('¥168.00');
    expect(element.textContent).toContain('订单支付');
    expect(element.textContent).toContain('SO202607080001');
    expect(element.textContent).toContain('查看订单');
  });

  it('shows retryable error state when merchant wallet logs fail to load', async () => {
    mockedCatalogService.getTenant.mockResolvedValue({
      id: 9,
      name: '本地测试店',
      status: 1,
    });
    mockedWalletService.getMerchantWallet
      .mockRejectedValueOnce(new Error('商户钱包服务不可用'))
      .mockResolvedValueOnce({
        walletType: 'MERCHANT',
        tenantId: 9,
        availableAmount: 88,
        frozenAmount: 0,
        totalRecharge: 100,
        totalConsume: 12,
      });
    mockedWalletService.getMerchantWalletLogs.mockResolvedValue({
      records: [],
      total: 0,
      page: 1,
      size: 10,
      pages: 0,
    });

    const element = await renderMerchantWalletDetail();

    expect(element.textContent).toContain('商户钱包服务不可用');
    expect(element.textContent).toContain('重试');

    const retryButton = Array.from(element.querySelectorAll('button'))
      .find((button) => button.textContent?.includes('重试'));
    expect(retryButton).toBeTruthy();

    await act(async () => {
      retryButton?.dispatchEvent(new MouseEvent('click', { bubbles: true }));
    });

    expect(mockedWalletService.getMerchantWallet).toHaveBeenCalledTimes(2);
    expect(element.textContent).toContain('¥88.00');
    expect(element.textContent).not.toContain('商户钱包服务不可用');
  });
});
