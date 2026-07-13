import React, { act } from 'react';
import { createRoot, type Root } from 'react-dom/client';
import { MemoryRouter } from 'react-router-dom';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
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
    listAssetHolds: vi.fn(),
  },
}));

const mockedWalletService = vi.mocked(appWalletService);

let root: Root | null = null;
let container: HTMLDivElement | null = null;

beforeEach(() => {
  mockedWalletService.listAssetHolds.mockResolvedValue([]);
});

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
    mockedWalletService.listAssetActivities.mockResolvedValue({
      records: [{
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
        sourceType: 'COUPON_WRITE_OFF_RECORD',
        sourceId: 101,
      }],
      nextCursor: 'cursor-2',
      hasMore: true,
    });
    mockedWalletService.listAssetHolds.mockResolvedValue([{
      tenantId: 9,
      assetType: 'POINTS',
      holdStatus: 'PRE_HOLD',
      amountText: '-20 积分',
      reason: '订单待支付',
      bizType: 'SALES_ORDER',
      bizNo: 'SO1001',
      occurredAt: '2026-07-10T10:30:00',
      actionPath: '/order/SO1001',
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
    expect(element.textContent).toContain('受限资产');
    expect(element.textContent).toContain('积分预占中');
    expect(element.textContent).toContain('SO1001');
  });

  it('shows a retryable error state when asset loading fails', async () => {
    mockedWalletService.getUnifiedWallet.mockRejectedValue(new Error('资产服务不可用'));
    mockedWalletService.getUnifiedWalletLogs.mockResolvedValue({ records: [], total: 0, page: 1, size: 5 });
    mockedWalletService.listTenantAssetSummaries.mockResolvedValue([]);
    mockedWalletService.listAssetActivities.mockResolvedValue({ records: [], nextCursor: null, hasMore: false });

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

  it('resets the activity cursor when filtering by asset type', async () => {
    mockWalletOverview();
    mockedWalletService.listAssetActivities
      .mockResolvedValueOnce({
        records: [{
          assetType: 'WALLET',
          title: '钱包消费',
          sourceType: 'UNIFIED_WALLET_LOG',
          sourceId: 1,
        }],
        nextCursor: 'cursor-2',
        hasMore: true,
      })
      .mockResolvedValueOnce({
        records: [{
          assetType: 'COUPON',
          title: '优惠券领取',
          sourceType: 'COUPON_RECEIVE_RECORD',
          sourceId: 2,
        }],
        nextCursor: null,
        hasMore: false,
      });

    const element = await renderWallet();
    const couponFilter = Array.from(element.querySelectorAll('button'))
      .find((button) => button.textContent === '优惠券');

    await act(async () => {
      couponFilter?.click();
    });

    expect(mockedWalletService.listAssetActivities).toHaveBeenLastCalledWith({
      types: ['COUPON'],
      cursor: undefined,
      size: 20,
    });
    expect(element.textContent).toContain('优惠券领取');
    expect(element.textContent).not.toContain('钱包消费');
  });

  it('keeps existing activity records when loading more fails', async () => {
    mockWalletOverview();
    mockedWalletService.listAssetActivities
      .mockResolvedValueOnce({
        records: [{
          assetType: 'POINTS',
          title: '积分入账',
          sourceType: 'MEMBER_POINTS_LOG',
          sourceId: 1,
        }],
        nextCursor: 'cursor-2',
        hasMore: true,
      })
      .mockRejectedValueOnce(new Error('加载更多失败'));

    const element = await renderWallet();
    const loadMore = Array.from(element.querySelectorAll('button'))
      .find((button) => button.textContent === '加载更多');

    await act(async () => {
      loadMore?.click();
    });

    expect(mockedWalletService.listAssetActivities).toHaveBeenLastCalledWith({
      size: 20,
      cursor: 'cursor-2',
    });
    expect(element.textContent).toContain('积分入账');
    expect(element.textContent).toContain('加载更多失败');
  });

  it('ignores an older filter response that resolves after a newer query', async () => {
    mockWalletOverview();
    const olderFilter = deferred<ActivityPage>();
    mockedWalletService.listAssetActivities
      .mockResolvedValueOnce(activityPage('初始动态', 1))
      .mockReturnValueOnce(olderFilter.promise)
      .mockResolvedValueOnce(activityPage('最新筛选动态', 3));

    const element = await renderWallet();

    await clickButton(element, '优惠券');
    await clickButton(element, '积分');

    expect(element.textContent).toContain('最新筛选动态');

    await act(async () => {
      olderFilter.resolve(activityPage('过期筛选动态', 2));
      await olderFilter.promise;
    });

    expect(element.textContent).toContain('最新筛选动态');
    expect(element.textContent).not.toContain('过期筛选动态');
  });

  it('does not append an older load-more response after the filters change', async () => {
    mockWalletOverview();
    const olderLoadMore = deferred<ActivityPage>();
    mockedWalletService.listAssetActivities
      .mockResolvedValueOnce(activityPage('初始动态', 1, 'cursor-2', true))
      .mockReturnValueOnce(olderLoadMore.promise)
      .mockResolvedValueOnce(activityPage('优惠券筛选动态', 3));

    const element = await renderWallet();

    await clickButton(element, '加载更多');
    await clickButton(element, '优惠券');

    await act(async () => {
      olderLoadMore.resolve(activityPage('过期追加动态', 2));
      await olderLoadMore.promise;
    });

    expect(element.textContent).toContain('优惠券筛选动态');
    expect(element.textContent).not.toContain('初始动态');
    expect(element.textContent).not.toContain('过期追加动态');
  });

  it('clears old activities and pagination when a non-append filter request fails', async () => {
    mockWalletOverview();
    mockedWalletService.listAssetActivities
      .mockResolvedValueOnce(activityPage('旧动态', 1, 'cursor-2', true))
      .mockRejectedValueOnce(new Error('筛选失败'));

    const element = await renderWallet();
    await clickButton(element, '优惠券');

    expect(element.textContent).toContain('筛选失败');
    expect(element.textContent).not.toContain('旧动态');
    expect(findButton(element, '加载更多')).toBeUndefined();
  });

  it('clears previous from and to values when CUSTOM is selected', async () => {
    mockWalletOverview();
    mockedWalletService.listAssetActivities.mockResolvedValue(activityPage('动态', 1));

    const element = await renderWallet();
    const timeRange = element.querySelector<HTMLSelectElement>('select[aria-label="时间范围筛选"]');

    await changeSelect(timeRange, 'SEVEN_DAYS');
    await changeSelect(timeRange, 'CUSTOM');
    await clickButton(element, '优惠券');

    expect(mockedWalletService.listAssetActivities).toHaveBeenLastCalledWith({
      types: ['COUPON'],
      cursor: undefined,
      size: 20,
      from: undefined,
      to: undefined,
    });
    expect(element.querySelector<HTMLInputElement>('input[aria-label="资产动态开始时间"]')?.value).toBe('');
    expect(element.querySelector<HTMLInputElement>('input[aria-label="资产动态结束时间"]')?.value).toBe('');
  });

  it('loads aggregated holds once and renders the tenant name returned by the endpoint', async () => {
    mockWalletOverview([tenantSummary(9, '一号店'), tenantSummary(10, '二号店')]);
    mockedWalletService.listAssetActivities.mockResolvedValue(activityPage('动态', 1));
    mockedWalletService.listAssetHolds.mockResolvedValue([{
      tenantId: 10,
      tenantName: '二号店',
      assetType: 'POINTS',
      holdStatus: 'PRE_HOLD',
      amountText: '-20 积分',
      reason: '订单待支付',
    }]);

    const element = await renderWallet();

    expect(mockedWalletService.listAssetHolds).toHaveBeenCalledTimes(1);
    expect(mockedWalletService.listAssetHolds).toHaveBeenCalledWith();
    expect(element.textContent).toContain('二号店 · -20 积分 · 订单待支付');
  });

  it('renders an activity without actionPath as non-interactive content', async () => {
    mockWalletOverview();
    mockedWalletService.listAssetActivities.mockResolvedValue(activityPage('只读动态', 1));

    const element = await renderWallet();
    const title = Array.from(element.querySelectorAll('div')).find((node) => node.textContent === '只读动态');

    expect(title?.closest('button')).toBeNull();
  });
});

interface ActivityPage {
  records: Array<{
    assetType: string;
    title: string;
    sourceType: string;
    sourceId: number;
  }>;
  nextCursor: string | null;
  hasMore: boolean;
}

function activityPage(title: string, sourceId: number, nextCursor: string | null = null, hasMore = false): ActivityPage {
  return {
    records: [{
      assetType: 'WALLET',
      title,
      sourceType: 'UNIFIED_WALLET_LOG',
      sourceId,
    }],
    nextCursor,
    hasMore,
  };
}

function tenantSummary(tenantId: number, tenantName: string) {
  return {
    tenantId,
    tenantName,
    walletAvailableAmount: 0,
    walletFrozenAmount: 0,
    points: 0,
    expiringSoonPoints: 0,
  };
}

function mockWalletOverview(tenantAssets = [] as ReturnType<typeof tenantSummary>[]) {
  mockedWalletService.getUnifiedWallet.mockResolvedValue({
    walletType: 'UNIFIED',
    tenantId: null,
    availableAmount: 500,
    frozenAmount: 0,
    totalRecharge: 500,
    totalConsume: 0,
  });
  mockedWalletService.getUnifiedWalletLogs.mockResolvedValue({ records: [], total: 0, page: 1, size: 5, pages: 0 });
  mockedWalletService.listTenantAssetSummaries.mockResolvedValue(tenantAssets);
}

function findButton(element: HTMLElement, text: string) {
  return Array.from(element.querySelectorAll('button')).find((button) => button.textContent === text);
}

async function clickButton(element: HTMLElement, text: string) {
  const button = findButton(element, text);
  expect(button).toBeDefined();
  await act(async () => {
    button?.click();
  });
}

async function changeSelect(select: HTMLSelectElement | null, value: string) {
  expect(select).not.toBeNull();
  await act(async () => {
    if (select) {
      select.value = value;
      select.dispatchEvent(new Event('change', { bubbles: true }));
    }
  });
}

function deferred<T>() {
  let resolve!: (value: T) => void;
  const promise = new Promise<T>((resolvePromise) => {
    resolve = resolvePromise;
  });
  return { promise, resolve };
}
