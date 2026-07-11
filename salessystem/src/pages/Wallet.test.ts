import React, { act } from 'react';
import { createRoot, type Root } from 'react-dom/client';
import { MemoryRouter } from 'react-router-dom';
import { afterEach, describe, expect, it, vi } from 'vitest';
import UserWallet from './Wallet';
import { appWalletService } from '../services/modules/appWallet';

vi.mock('motion/react', () => ({
  motion: new Proxy({}, {
    get: (_target, tag: string) => tag,
  }),
}));

vi.mock('../services/modules/appWallet', () => ({
  appWalletService: {
    getUnifiedWallet: vi.fn(),
    getUnifiedWalletLogs: vi.fn(),
    listTenantAssetSummaries: vi.fn(),
    listAssetActivities: vi.fn(),
  },
}));

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

async function renderWallet() {
  container = document.createElement('div');
  document.body.appendChild(container);
  root = createRoot(container);

  await act(async () => {
    root?.render(
      React.createElement(
        MemoryRouter,
        null,
        React.createElement(UserWallet),
      ),
    );
  });

  return container;
}

describe('UserWallet', () => {
  it('shows merchant wallet detail entry for tenant assets', async () => {
    mockedWalletService.getUnifiedWallet.mockResolvedValue({
      walletType: 'UNIFIED',
      tenantId: null,
      availableAmount: 500,
      frozenAmount: 0,
      totalRecharge: 500,
      totalConsume: 0,
    });
    mockedWalletService.getUnifiedWalletLogs.mockResolvedValue({ records: [], total: 0, page: 1, size: 5, pages: 0 });
    mockedWalletService.listTenantAssetSummaries.mockResolvedValue([{
      tenantId: 9,
      tenantName: '本地测试店',
      walletAvailableAmount: 120,
      walletFrozenAmount: 0,
      points: 88,
      expiringSoonPoints: 0,
      usableCouponCount: 3,
      lockedCouponCount: 1,
      usedCouponCount: 2,
      expiredCouponCount: 4,
      expiringSoonCouponCount: 2,
      totalGrowth: 260,
    }]);
    mockedWalletService.listAssetActivities.mockResolvedValue([{
      assetType: 'COUPON',
      title: '优惠券核销',
      description: '订单 SO1001 已使用优惠券',
      tenantId: 9,
      tenantName: '本地测试店',
      bizNo: 'SO1001',
      amountText: '¥8',
      tone: 'positive',
      occurredAt: '2026-07-10T10:00:00',
      actionPath: '/coupons?tenantId=9',
    }]);

    const element = await renderWallet();

    expect(element.textContent).toContain('本地测试店');
    expect(element.textContent).toContain('商户钱包 ¥120.00');
    expect(element.textContent).toContain('可用券 3 张');
    expect(element.textContent).toContain('成长值 260');
    expect(element.textContent).toContain('资产提醒');
    expect(element.textContent).toContain('优惠券锁定中');
    expect(element.textContent).toContain('优惠券即将过期');
    expect(element.textContent).toContain('统一资产动态');
    expect(element.textContent).toContain('优惠券核销');
    expect(element.textContent).toContain('钱包明细');
  });

  it('shows a retryable error state when asset loading fails', async () => {
    mockedWalletService.getUnifiedWallet.mockRejectedValue(new Error('资产服务不可用'));
    mockedWalletService.getUnifiedWalletLogs.mockResolvedValue({ records: [], total: 0, page: 1, size: 5 });
    mockedWalletService.listTenantAssetSummaries.mockResolvedValue([]);
    mockedWalletService.listAssetActivities.mockResolvedValue([]);

    const element = await renderWallet();

    expect(element.textContent).toContain('资产服务不可用');
    expect(element.textContent).toContain('重试');
  });

  it('keeps wallet balances and summaries available when the optional activity feed fails', async () => {
    mockedWalletService.getUnifiedWallet.mockResolvedValue({
      walletType: 'UNIFIED',
      tenantId: null,
      availableAmount: 500,
      frozenAmount: 0,
      totalRecharge: 500,
      totalConsume: 0,
    });
    mockedWalletService.getUnifiedWalletLogs.mockResolvedValue({ records: [], total: 0, page: 1, size: 5, pages: 0 });
    mockedWalletService.listTenantAssetSummaries.mockResolvedValue([{
      tenantId: 9,
      tenantName: '本地测试店',
      walletAvailableAmount: 120,
      walletFrozenAmount: 0,
      points: 88,
      expiringSoonPoints: 0,
      usableCouponCount: 3,
      totalGrowth: 260,
    }]);
    mockedWalletService.listAssetActivities.mockRejectedValue(new Error('资产动态服务不可用'));

    const element = await renderWallet();

    expect(element.textContent).toContain('¥500.00');
    expect(element.textContent).toContain('本地测试店');
    expect(element.textContent).toContain('资产动态暂时不可用');
    expect(element.textContent).not.toContain('钱包资产加载失败');
  });
});
