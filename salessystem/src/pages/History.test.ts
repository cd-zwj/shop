import React, { act } from 'react';
import { createRoot, type Root } from 'react-dom/client';
import { MemoryRouter } from 'react-router-dom';
import { afterEach, describe, expect, it, vi } from 'vitest';
import ConsumptionHistory from './History';
import { appWalletService } from '../services/modules/appWallet';

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
});
