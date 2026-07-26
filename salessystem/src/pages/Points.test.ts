import React, { act } from 'react';
import { createRoot, type Root } from 'react-dom/client';
import { MemoryRouter, Route, Routes } from 'react-router-dom';
import { afterEach, describe, expect, it, vi } from 'vitest';
import Points from './Points';
import { ToastProvider } from '../context/ToastContext';
import { appPointsService } from '../services/modules/appPoints';

vi.mock('motion/react', () => ({
  AnimatePresence: ({ children }: { children: React.ReactNode }) => children,
  motion: new Proxy({}, {
    get: (_target, tag: string) => tag,
  }),
}));

vi.mock('../services/modules/appPoints', () => ({
  appPointsService: {
    getPointsBalance: vi.fn(),
    getPointsLogs: vi.fn(),
  },
}));

const mockedPointsService = vi.mocked(appPointsService);

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

async function renderPoints() {
  container = document.createElement('div');
  document.body.appendChild(container);
  root = createRoot(container);

  await act(async () => {
    root?.render(
      React.createElement(
        MemoryRouter,
        { initialEntries: ['/points/9'] },
        React.createElement(
          ToastProvider,
          null,
          React.createElement(
            Routes,
            null,
            React.createElement(Route, { path: '/points/:tenantId', element: React.createElement(Points) }),
          ),
        ),
      ),
    );
  });

  return container;
}

describe('Points', () => {
  it('shows a retryable error state when points assets fail to load', async () => {
    mockedPointsService.getPointsBalance
      .mockRejectedValueOnce(new Error('积分服务不可用'))
      .mockResolvedValueOnce({
        id: 1,
        points: 120,
        totalEarned: 200,
        totalUsed: 80,
        expiringSoonPoints: 0,
        status: 1,
        createTime: '2026-07-06T10:00:00',
        updateTime: '2026-07-06T10:00:00',
      });
    mockedPointsService.getPointsLogs.mockResolvedValue({ records: [], total: 0, page: 1, size: 20, pages: 0 });

    const element = await renderPoints();

    expect(element.textContent).toContain('积分服务不可用');
    expect(element.textContent).toContain('重试');

    const retryButton = Array.from(element.querySelectorAll('button'))
      .find((button) => button.textContent?.includes('重试'));
    expect(retryButton).toBeTruthy();

    await act(async () => {
      retryButton?.dispatchEvent(new MouseEvent('click', { bubbles: true }));
    });

    expect(element.textContent).toContain('120');
    expect(Array.from(element.querySelectorAll('button'))
      .some((button) => button.textContent?.includes('重试'))).toBe(false);
  });
});
