import React, { act } from 'react';
import { createRoot, type Root } from 'react-dom/client';
import { MemoryRouter, Route, Routes } from 'react-router-dom';
import { afterEach, describe, expect, it, vi } from 'vitest';
import PaymentStatus from './PaymentStatus';
import { CartProvider } from '../context/CartContext';
import { appOrderService } from '../services/modules/appOrder';
import { appPaymentBillService } from '../services/modules/appPaymentBill';

vi.mock('motion/react', () => ({
  motion: new Proxy({}, {
    get: (_target, tag: string) => tag,
  }),
}));

vi.mock('../services/modules/appPaymentBill', () => ({
  appPaymentBillService: {
    getLatestPaymentBillByBiz: vi.fn(),
    getPaymentBill: vi.fn(),
    syncPaymentBill: vi.fn(),
  },
}));

vi.mock('../services/modules/appOrder', () => ({
  appOrderService: {
    getOrder: vi.fn(),
    repayOrder: vi.fn(),
  },
}));

const mockedPaymentBillService = vi.mocked(appPaymentBillService);
const mockedOrderService = vi.mocked(appOrderService);

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
  });
}

async function renderPaymentStatus() {
  container = document.createElement('div');
  document.body.appendChild(container);
  root = createRoot(container);

  await act(async () => {
    root?.render(
      React.createElement(
        MemoryRouter,
        { initialEntries: ['/payment/status?billNo=PB001&orderNo=SO001&source=order'] },
        React.createElement(
          CartProvider,
          null,
          React.createElement(
            Routes,
            null,
            React.createElement(Route, { path: '/payment/status', element: React.createElement(PaymentStatus) }),
          ),
        ),
      ),
    );
  });

  await flushAsyncWork();
  return container;
}

describe('PaymentStatus', () => {
  it('shows a retryable query error without treating it as payment failure', async () => {
    mockedPaymentBillService.getPaymentBill
      .mockRejectedValueOnce(new Error('支付单查询超时'))
      .mockResolvedValueOnce({
        id: 1,
        billNo: 'PB001',
        bizType: 'ORDER',
        bizNo: 'SO001',
        tenantId: 9,
        platformUserId: 3,
        channelCode: 'ALIPAY_PAGE',
        channelMode: 'PAGE',
        payAmount: 9900,
        payStatus: 'PAYING',
        thirdPartyBillNo: null,
        callbackStatus: 'INIT',
        statusRemark: null,
        expireTime: null,
        extensionJson: null,
        createTime: '2026-07-06T10:00:00',
        updateTime: '2026-07-06T10:01:00',
      });

    const element = await renderPaymentStatus();

    expect(element.textContent).toContain('支付状态查询失败');
    expect(element.textContent).toContain('支付单查询超时');
    expect(element.textContent).not.toContain('重新支付');

    const retryButton = Array.from(element.querySelectorAll('button'))
      .find((button) => button.textContent?.includes('重试查询'));
    expect(retryButton).toBeTruthy();

    await act(async () => {
      retryButton?.dispatchEvent(new MouseEvent('click', { bubbles: true }));
    });
    await flushAsyncWork();

    expect(mockedPaymentBillService.getPaymentBill).toHaveBeenCalledTimes(2);
    expect(element.textContent).toContain('交易确认中');
    expect(element.textContent).not.toContain('支付状态查询失败');
  });

  it('shows retry and merchant contact actions when order payment fails', async () => {
    mockedPaymentBillService.getPaymentBill.mockResolvedValue({
      id: 1,
      billNo: 'PB001',
      bizType: 'ORDER',
      bizNo: 'SO001',
      tenantId: 9,
      platformUserId: 3,
      channelCode: 'ALIPAY_PAGE',
      channelMode: 'PAGE',
      payAmount: 9900,
      payStatus: 'FAILED',
      thirdPartyBillNo: null,
      callbackStatus: 'FAILED',
      statusRemark: '渠道返回：余额不足',
      expireTime: null,
      extensionJson: null,
      createTime: '2026-07-06T10:00:00',
      updateTime: '2026-07-06T10:01:00',
    });
    mockedOrderService.repayOrder.mockResolvedValue({
      orderNo: 'SO001',
      orderStatus: 'CREATED',
      payStatus: 'PAYING',
      totalAmount: 9900,
      unifiedWalletDeductAmount: 0,
      merchantWalletDeductAmount: 0,
      externalPayAmount: 9900,
      paymentBillNo: 'PB002',
      externalPayUrl: null,
      reusedPaymentBill: false,
    });

    const element = await renderPaymentStatus();

    expect(element.textContent).toContain('失败原因：渠道返回：余额不足');
    expect(element.textContent).toContain('重新支付');
    expect(Array.from(element.querySelectorAll('button'))
      .some((button) => button.textContent?.includes('联系商户'))).toBe(true);
  });
});
