import React, { act } from 'react';
import { createRoot, type Root } from 'react-dom/client';
import { MemoryRouter, Route, Routes } from 'react-router-dom';
import { afterEach, describe, expect, it, vi } from 'vitest';
import GrowthCenter from './GrowthCenter';
import { ToastProvider } from '../context/ToastContext';
import { appGrowthService } from '../services/modules/appGrowth';

vi.mock('motion/react', () => ({
  AnimatePresence: ({ children }: { children: React.ReactNode }) => children,
  motion: new Proxy({}, {
    get: (_target, tag: string) => tag,
  }),
}));

vi.mock('../services/modules/appGrowth', () => ({
  appGrowthService: {
    getGrowthOverview: vi.fn(),
    getGrowthLogs: vi.fn(),
  },
}));

const mockedGrowthService = vi.mocked(appGrowthService);

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

async function renderGrowthCenter() {
  container = document.createElement('div');
  document.body.appendChild(container);
  root = createRoot(container);

  await act(async () => {
    root?.render(
      React.createElement(
        MemoryRouter,
        { initialEntries: ['/growth/9'] },
        React.createElement(
          ToastProvider,
          null,
          React.createElement(
            Routes,
            null,
            React.createElement(Route, { path: '/growth/:tenantId', element: React.createElement(GrowthCenter) }),
          ),
        ),
      ),
    );
  });

  return container;
}

describe('GrowthCenter', () => {
  it('shows a retryable error state when growth assets fail to load', async () => {
    mockedGrowthService.getGrowthOverview
      .mockRejectedValueOnce(new Error('成长值服务不可用'))
      .mockResolvedValueOnce({
        totalGrowth: 360,
        levelId: 2,
        levelName: '银卡会员',
        nextLevelGrowth: 500,
      });
    mockedGrowthService.getGrowthLogs.mockResolvedValue({
      records: [],
      total: 0,
      page: 1,
      size: 20,
      pages: 0,
    });

    const element = await renderGrowthCenter();

    expect(element.textContent).toContain('成长值服务不可用');
    expect(element.textContent).toContain('重试');

    const retryButton = Array.from(element.querySelectorAll('button'))
      .find((button) => button.textContent?.includes('重试'));
    expect(retryButton).toBeTruthy();

    await act(async () => {
      retryButton?.dispatchEvent(new MouseEvent('click', { bubbles: true }));
    });

    expect(element.textContent).toContain('360');
    expect(element.textContent).toContain('银卡会员');
    expect(Array.from(element.querySelectorAll('button'))
      .some((button) => button.textContent?.includes('重试'))).toBe(false);
  });
});
