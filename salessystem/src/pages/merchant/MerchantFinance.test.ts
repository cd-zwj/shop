import React, { act } from 'react';
import { createRoot, type Root } from 'react-dom/client';
import { MemoryRouter, Route, Routes } from 'react-router-dom';
import { afterEach, describe, expect, it, vi } from 'vitest';
import MerchantFinance from './MerchantFinance';
import { merchantFinanceService } from '../../services/modules/merchantFinance';

vi.mock('../../context/AuthContext', () => ({
  useAuth: () => ({
    merchantSession: {
      tenantId: 9,
      tenantName: '测试店铺',
      employeeRole: 'OWNER',
    },
  }),
}));

vi.mock('../../services/modules/merchantFinance', () => ({
  merchantFinanceService: {
    getWalletSummary: vi.fn(),
    getPointsRule: vi.fn(),
    listRechargeRules: vi.fn(),
    listWithdrawals: vi.fn(),
    listTransactions: vi.fn(),
  },
}));

const mockedFinanceService = vi.mocked(merchantFinanceService);

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

async function renderMerchantFinance() {
  container = document.createElement('div');
  document.body.appendChild(container);
  root = createRoot(container);

  await act(async () => {
    root?.render(
      React.createElement(
        MemoryRouter,
        { initialEntries: ['/merchant/finance'] },
        React.createElement(
          Routes,
          null,
          React.createElement(Route, {
            path: '/merchant/finance',
            element: React.createElement(MerchantFinance),
          }),
        ),
      ),
    );
  });

  await flushAsyncWork();
  return container;
}

describe('MerchantFinance', () => {
  it('shows a retryable transaction error state without hiding the finance summary', async () => {
    mockedFinanceService.getWalletSummary.mockResolvedValue({
      tenantId: 9,
      availableBalance: 1200,
      frozenBalance: 100,
      totalIncome: 5000,
      totalWithdrawal: 800,
    });
    mockedFinanceService.getPointsRule.mockResolvedValue({ pointsRatio: 1, enabled: true });
    mockedFinanceService.listRechargeRules.mockResolvedValue([]);
    mockedFinanceService.listWithdrawals.mockResolvedValue({
      records: [],
      total: 0,
      page: 1,
      size: 6,
      pages: 0,
    });
    mockedFinanceService.listTransactions
      .mockRejectedValueOnce(new Error('流水接口不可用'))
      .mockResolvedValueOnce({
        records: [{
          id: 1,
          bizType: 'PAYMENT',
          bizNo: 'SO202607080001',
          changeAmount: 88,
          balanceBefore: 1112,
          balanceAfter: 1200,
          remark: '订单收款',
          createTime: '2026-07-08T10:00:00',
        }],
        total: 1,
        page: 1,
        size: 10,
        pages: 1,
      });

    const element = await renderMerchantFinance();

    expect(element.textContent).toContain('¥1,200.00');
    expect(element.textContent).toContain('流水接口不可用');
    expect(element.textContent).toContain('重试');
    expect(mockedFinanceService.listTransactions).toHaveBeenCalledTimes(1);

    const retryButton = Array.from(element.querySelectorAll('button'))
      .find((button) => button.textContent?.includes('重试'));
    expect(retryButton).toBeTruthy();

    await act(async () => {
      retryButton?.dispatchEvent(new MouseEvent('click', { bubbles: true }));
    });
    await flushAsyncWork();

    expect(mockedFinanceService.listTransactions).toHaveBeenCalledTimes(2);
    expect(element.textContent).toContain('SO202607080001');
    expect(element.textContent).toContain('订单收款');
    expect(element.textContent).not.toContain('流水接口不可用');
  });
});
