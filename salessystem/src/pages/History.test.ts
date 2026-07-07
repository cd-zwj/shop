import React, { act } from 'react';
import { createRoot, type Root } from 'react-dom/client';
import { MemoryRouter } from 'react-router-dom';
import { afterEach, describe, expect, it, vi } from 'vitest';
import ConsumptionHistory from './History';
import { appWalletService } from '../services/modules/appWallet';
import type { WalletLog } from '../types/wallet';

vi.mock('motion/react', () => ({
  motion: new Proxy({}, {
    get: (_target, tag: string) => tag,
  }),
}));

vi.mock('../services/modules/appWallet', () => ({
  appWalletService: {
    getUnifiedWalletLogs: vi.fn(),
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

async function flushAsyncWork() {
  await act(async () => {
    await Promise.resolve();
    await Promise.resolve();
  });
}

async function renderHistory() {
  container = document.createElement('div');
  document.body.appendChild(container);
  root = createRoot(container);

  await act(async () => {
    root?.render(
      React.createElement(
        MemoryRouter,
        null,
        React.createElement(ConsumptionHistory),
      ),
    );
  });

  await flushAsyncWork();
  return container;
}

describe('ConsumptionHistory', () => {
  it('shows a retryable error state when wallet logs fail to load', async () => {
    mockedWalletService.getUnifiedWalletLogs.mockRejectedValueOnce(new Error('流水服务不可用'));

    const element = await renderHistory();

    expect(element.textContent).toContain('流水服务不可用');
    expect(element.textContent).toContain('重试');
    expect(element.textContent).toContain('最近流水');
  });

  it('reloads wallet logs after retrying a failed request', async () => {
    mockedWalletService.getUnifiedWalletLogs
      .mockRejectedValueOnce(new Error('流水服务不可用'))
      .mockResolvedValueOnce({
        records: [buildWalletLog({
          bizType: 'ORDER_PAY',
          bizNo: 'SO202607080001',
          changeAmount: -128,
          balanceAfter: 872,
          remark: '购买虚拟商品',
        })],
        total: 1,
        page: 1,
        size: 20,
        pages: 1,
      });

    const element = await renderHistory();

    expect(element.textContent).toContain('流水服务不可用');
    const retryButton = Array.from(element.querySelectorAll('button'))
      .find((button) => button.textContent?.includes('重试'));
    expect(retryButton).toBeTruthy();

    await act(async () => {
      retryButton?.dispatchEvent(new MouseEvent('click', { bubbles: true }));
    });
    await flushAsyncWork();

    expect(element.textContent).toContain('SO202607080001');
    expect(element.textContent).toContain('购买虚拟商品');
    expect(element.textContent).not.toContain('流水服务不可用');
  });

  it('exports the visible wallet logs as csv', async () => {
    const createObjectURL = vi.fn(() => 'blob:wallet-logs');
    const revokeObjectURL = vi.fn();
    Object.defineProperty(URL, 'createObjectURL', { configurable: true, value: createObjectURL });
    Object.defineProperty(URL, 'revokeObjectURL', { configurable: true, value: revokeObjectURL });
    const clickSpy = vi.spyOn(HTMLAnchorElement.prototype, 'click').mockImplementation(() => undefined);

    mockedWalletService.getUnifiedWalletLogs.mockResolvedValue({
      records: [buildWalletLog({
        bizType: 'RECHARGE',
        bizNo: 'RC202607080001',
        changeAmount: 500,
        balanceAfter: 1500,
        remark: '本地充值',
      })],
      total: 1,
      page: 1,
      size: 20,
      pages: 1,
    });

    const element = await renderHistory();
    const exportButton = Array.from(element.querySelectorAll('button'))
      .find((button) => button.textContent?.includes('导出 CSV'));
    expect(exportButton).toBeTruthy();

    await act(async () => {
      exportButton?.dispatchEvent(new MouseEvent('click', { bubbles: true }));
    });

    expect(createObjectURL).toHaveBeenCalledTimes(1);
    expect(clickSpy).toHaveBeenCalledTimes(1);
    expect(revokeObjectURL).toHaveBeenCalledWith('blob:wallet-logs');

    clickSpy.mockRestore();
  });
});

function buildWalletLog(overrides: Partial<WalletLog>): WalletLog {
  return {
    walletType: 'UNIFIED',
    tenantId: null,
    bizType: 'ORDER_PAY',
    bizNo: 'SO202607080000',
    changeAmount: -100,
    balanceBefore: 1000,
    balanceAfter: 900,
    remark: '测试流水',
    createTime: '2026-07-08T10:00:00',
    ...overrides,
  };
}
