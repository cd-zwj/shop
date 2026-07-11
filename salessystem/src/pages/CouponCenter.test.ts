import React, { act } from 'react';
import { createRoot, type Root } from 'react-dom/client';
import { MemoryRouter, Route, Routes } from 'react-router-dom';
import { afterEach, describe, expect, it, vi } from 'vitest';
import CouponCenter from './CouponCenter';
import { ToastProvider } from '../context/ToastContext';
import { appCatalogService } from '../services/modules/appCatalog';
import { appCouponService } from '../services/modules/appCoupon';

vi.mock('motion/react', () => ({
  AnimatePresence: ({ children }: { children: React.ReactNode }) => children,
  motion: new Proxy({}, {
    get: (_target, tag: string) => tag,
  }),
}));

vi.mock('../services/modules/appCatalog', () => ({
  appCatalogService: {
    listTenants: vi.fn(),
  },
}));

vi.mock('../services/modules/appCoupon', () => ({
  appCouponService: {
    getAvailableCoupons: vi.fn(),
    getMyCoupons: vi.fn(),
    claimCoupon: vi.fn(),
  },
}));

const mockedCatalogService = vi.mocked(appCatalogService);
const mockedCouponService = vi.mocked(appCouponService);

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

async function renderCouponCenter(initialEntry = '/coupons?tenantId=9') {
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
            React.createElement(Route, { path: '/coupons', element: React.createElement(CouponCenter) }),
          ),
        ),
      ),
    );
  });

  await flushAsyncWork();
  return container;
}

describe('CouponCenter', () => {
  it('shows a summarized coupon usage timeline', async () => {
    mockedCatalogService.listTenants.mockResolvedValue([{ id: 9, name: '测试店铺' }]);
    mockedCouponService.getAvailableCoupons.mockResolvedValue([]);
    mockedCouponService.getMyCoupons.mockResolvedValue([{
      id: 21,
      couponNo: 'UC21',
      couponTemplateId: 11,
      tenantId: 9,
      status: 'USED',
      name: '满 100 减 20',
      couponType: 'FIXED',
      thresholdAmount: 100,
      discountAmount: 20,
      discountRate: null,
      maxDiscountAmount: null,
      receiveTime: '2026-07-01T10:00:00',
      expireTime: '2026-08-01T10:00:00',
      usedTime: '2026-07-03T10:00:00',
      orderNo: 'SO20260703001',
      timeline: [
        { eventType: 'RECEIVE', title: '已领取', description: '优惠券已进入账户', occurredAt: '2026-07-01T10:00:00' },
        { eventType: 'LOCK', title: '已锁定', description: '订单 SO20260703001 已锁定优惠券', occurredAt: '2026-07-03T09:55:00', orderNo: 'SO20260703001' },
        { eventType: 'WRITE_OFF', title: '已核销', description: '订单 SO20260703001 已使用优惠券，抵扣 ¥20', occurredAt: '2026-07-03T10:00:00', orderNo: 'SO20260703001', amount: 20 },
      ],
    }]);

    const element = await renderCouponCenter();

    expect(element.textContent).toContain('优惠券使用时间线');
    expect(element.textContent).toContain('共 3 条事件');
    expect(element.textContent).toContain('已核销 · 满 100 减 20');
    expect(element.textContent).toContain('订单 SO20260703001 已使用优惠券');

    const expiredButton = Array.from(element.querySelectorAll('button'))
      .find((button) => button.textContent?.includes('查看失效记录'));
    await act(async () => {
      expiredButton?.dispatchEvent(new MouseEvent('click', { bubbles: true }));
    });
    await flushAsyncWork();

    const timelineButton = Array.from(element.querySelectorAll('button'))
      .find((button) => button.textContent?.includes('查看时间线'));
    expect(timelineButton).toBeTruthy();
    await act(async () => {
      timelineButton?.dispatchEvent(new MouseEvent('click', { bubbles: true }));
    });
    await flushAsyncWork();

    expect(element.textContent).toContain('生命周期明细');
  });

  it('shows a retryable error state when coupon assets fail to load', async () => {
    mockedCatalogService.listTenants.mockResolvedValue([{ id: 9, name: '测试店铺' }]);
    mockedCouponService.getAvailableCoupons
      .mockRejectedValueOnce(new Error('优惠券服务不可用'))
      .mockResolvedValueOnce([{
        id: 11,
        tenantId: 9,
        ownerType: 'TENANT',
        name: '满 100 减 20',
        couponType: 'FIXED',
        thresholdAmount: 100,
        discountAmount: 20,
        discountRate: null,
        maxDiscountAmount: null,
        perUserLimit: 1,
        remainingStock: 8,
        receivedByCurrentUser: 0,
        receivable: true,
        receiveStartTime: '2026-07-01T00:00:00',
        receiveEndTime: '2026-07-31T23:59:59',
        validStartTime: '2026-07-01T00:00:00',
        validEndTime: '2026-08-31T23:59:59',
        validDaysAfterReceive: null,
        description: null,
      }]);
    mockedCouponService.getMyCoupons.mockResolvedValue([]);

    const element = await renderCouponCenter();

    expect(element.textContent).toContain('优惠券服务不可用');
    expect(element.textContent).toContain('重试');

    const retryButton = Array.from(element.querySelectorAll('button'))
      .find((button) => button.textContent?.includes('重试'));
    expect(retryButton).toBeTruthy();

    await act(async () => {
      retryButton?.dispatchEvent(new MouseEvent('click', { bubbles: true }));
    });
    await flushAsyncWork();

    expect(element.textContent).toContain('满 100 减 20');
    expect(Array.from(element.querySelectorAll('button'))
      .some((button) => button.textContent?.includes('重试'))).toBe(false);
  });

  it('opens the requested coupon tab and keeps locked coupons in my coupons', async () => {
    mockedCatalogService.listTenants.mockResolvedValue([{ id: 9, name: '测试店铺' }]);
    mockedCouponService.getAvailableCoupons.mockResolvedValue([]);
    mockedCouponService.getMyCoupons.mockResolvedValue([{
      id: 22,
      couponNo: 'UC22',
      couponTemplateId: 12,
      tenantId: 9,
      status: 'LOCKED',
      name: '锁定优惠券',
      couponType: 'FIXED',
      thresholdAmount: 50,
      discountAmount: 10,
      discountRate: null,
      maxDiscountAmount: null,
      receiveTime: '2026-07-01T10:00:00',
      expireTime: '2026-08-01T10:00:00',
      usedTime: null,
      orderNo: 'SO20260703002',
      timeline: [],
    }]);

    const element = await renderCouponCenter('/coupons?tenantId=9&tab=my');
    const myCouponTab = Array.from(element.querySelectorAll('button'))
      .find((button) => button.textContent?.includes('我的券'));

    expect(myCouponTab?.className).toContain('border-primary');
    expect(element.textContent).toContain('锁定优惠券');
    expect(element.textContent).toContain('锁定中');
  });
});
