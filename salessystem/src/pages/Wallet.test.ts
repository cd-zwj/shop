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
  it('shows a retryable error state when asset loading fails', async () => {
    mockedWalletService.getUnifiedWallet.mockRejectedValue(new Error('资产服务不可用'));
    mockedWalletService.getUnifiedWalletLogs.mockResolvedValue({ records: [], total: 0, page: 1, size: 5 });
    mockedWalletService.listTenantAssetSummaries.mockResolvedValue([]);

    const element = await renderWallet();

    expect(element.textContent).toContain('资产服务不可用');
    expect(element.textContent).toContain('重试');
  });
});
