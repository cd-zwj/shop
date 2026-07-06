import React, { act } from 'react';
import { createRoot, type Root } from 'react-dom/client';
import { MemoryRouter, Route, Routes } from 'react-router-dom';
import { afterEach, describe, expect, it, vi } from 'vitest';
import Notifications from './Notifications';
import { ToastProvider } from '../context/ToastContext';
import { appNotificationService } from '../services/modules/appNotification';
import type { AppNotification } from '../types/addressNotification';

vi.mock('motion/react', () => ({
  AnimatePresence: ({ children }: { children: React.ReactNode }) => children,
  motion: new Proxy({}, {
    get: (_target, tag: string) => tag,
  }),
}));

vi.mock('../services/modules/appNotification', () => ({
  appNotificationService: {
    list: vi.fn(),
    markRead: vi.fn(),
    markAllRead: vi.fn(),
    getUnreadCount: vi.fn(),
  },
}));

const mockedNotificationService = vi.mocked(appNotificationService);

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

async function renderNotifications() {
  container = document.createElement('div');
  document.body.appendChild(container);
  root = createRoot(container);

  await act(async () => {
    root?.render(
      React.createElement(
        MemoryRouter,
        { initialEntries: ['/notifications'] },
        React.createElement(
          ToastProvider,
          null,
          React.createElement(
            Routes,
            null,
            React.createElement(Route, { path: '/notifications', element: React.createElement(Notifications) }),
            React.createElement(Route, { path: '/order/:orderNo', element: React.createElement('div', null, '订单详情路由') }),
          ),
        ),
      ),
    );
  });

  await flushAsyncWork();
  return container;
}

describe('Notifications', () => {
  it('marks an unread actionable notification as read before navigating', async () => {
    const notification = buildNotification({
      id: 8,
      category: 'ORDER',
      title: '订单已支付',
      content: '您的订单 SO202607060001 已支付成功',
      readStatus: 0,
    });
    let resolveMarkRead: (value: AppNotification) => void = () => undefined;
    const markReadPromise = new Promise<AppNotification>((resolve) => {
      resolveMarkRead = resolve;
    });

    mockedNotificationService.list.mockResolvedValue({
      records: [notification],
      total: 1,
      page: 1,
      size: 20,
      pages: 1,
    });
    mockedNotificationService.getUnreadCount.mockResolvedValue({ count: 1 });
    mockedNotificationService.markRead.mockReturnValue(markReadPromise);

    const element = await renderNotifications();
    const actionButton = Array.from(element.querySelectorAll('button'))
      .find((button) => button.textContent?.includes('查看订单'));
    expect(actionButton).toBeTruthy();

    await act(async () => {
      actionButton?.dispatchEvent(new MouseEvent('click', { bubbles: true }));
    });

    expect(mockedNotificationService.markRead).toHaveBeenCalledWith(8);
    expect(element.textContent).not.toContain('订单详情路由');

    await act(async () => {
      resolveMarkRead({ ...notification, readStatus: 1, readTime: '2026-07-06T10:00:00' });
      await markReadPromise;
    });
    await flushAsyncWork();

    expect(element.textContent).toContain('订单详情路由');
  });

  it('deduplicates concurrent mark-read requests for the same notification', async () => {
    const notification = buildNotification({
      id: 9,
      title: '系统通知',
      content: '请查看',
      readStatus: 0,
    });
    let resolveMarkRead: (value: AppNotification) => void = () => undefined;
    const markReadPromise = new Promise<AppNotification>((resolve) => {
      resolveMarkRead = resolve;
    });

    mockedNotificationService.list.mockResolvedValue({
      records: [notification],
      total: 1,
      page: 1,
      size: 20,
      pages: 1,
    });
    mockedNotificationService.getUnreadCount.mockResolvedValue({ count: 1 });
    mockedNotificationService.markRead.mockReturnValue(markReadPromise);

    const element = await renderNotifications();
    const card = Array.from(element.querySelectorAll('.cursor-pointer'))[0];
    expect(card).toBeTruthy();

    await act(async () => {
      card.dispatchEvent(new MouseEvent('click', { bubbles: true }));
      card.dispatchEvent(new MouseEvent('click', { bubbles: true }));
    });

    expect(mockedNotificationService.markRead).toHaveBeenCalledTimes(1);

    await act(async () => {
      resolveMarkRead({ ...notification, readStatus: 1, readTime: '2026-07-06T10:00:00' });
      await markReadPromise;
    });
    await flushAsyncWork();

    expect(element.textContent).not.toContain('1 条未读');
  });
});

function buildNotification(overrides: Partial<AppNotification>): AppNotification {
  return {
    id: 1,
    platformUserId: 2,
    title: '通知',
    content: '内容',
    category: 'SYSTEM',
    readStatus: 0,
    deleted: 0,
    readTime: null,
    createTime: '2026-07-06T10:00:00',
    updateTime: null,
    ...overrides,
  };
}
