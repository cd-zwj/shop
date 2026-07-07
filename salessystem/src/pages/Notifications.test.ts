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
  it('shows a retryable error state when notifications fail to load', async () => {
    const notification = buildNotification({
      id: 11,
      title: '退款处理完成',
      content: '您的退款已原路退回',
      readStatus: 1,
    });

    mockedNotificationService.list
      .mockRejectedValueOnce(new Error('通知服务暂不可用'))
      .mockResolvedValue({
        records: [notification],
        total: 1,
        page: 1,
        size: 20,
        pages: 1,
      });
    mockedNotificationService.getUnreadCount.mockResolvedValue({ count: 0 });

    const element = await renderNotifications();

    expect(element.textContent).toContain('通知加载失败');
    expect(element.textContent).toContain('通知服务暂不可用');
    expect(mockedNotificationService.list).toHaveBeenCalledTimes(1);

    const retryButton = Array.from(element.querySelectorAll('button'))
      .find((button) => button.textContent?.includes('重试'));
    expect(retryButton).toBeTruthy();

    await act(async () => {
      retryButton?.dispatchEvent(new MouseEvent('click', { bubbles: true }));
    });
    await flushAsyncWork();

    expect(mockedNotificationService.list).toHaveBeenCalledTimes(2);
    expect(element.textContent).toContain('退款处理完成');
    expect(element.textContent).not.toContain('通知加载失败');
  });

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

  it('derives pagination from total and size when pages is missing', async () => {
    mockedNotificationService.list.mockResolvedValue({
      records: [buildNotification({ id: 21, title: '分页通知' })],
      total: 21,
      page: 1,
      size: 20,
    });
    mockedNotificationService.getUnreadCount.mockResolvedValue({ count: 0 });

    const element = await renderNotifications();

    expect(element.textContent).toContain('1 / 2');
    const nextButton = element.querySelector('button[aria-label="下一页"]');
    expect(nextButton).toBeTruthy();
  });

  it('marks all unread notifications even when the current page has no unread items', async () => {
    mockedNotificationService.list.mockResolvedValue({
      records: [buildNotification({ id: 30, title: '已读通知', readStatus: 1 })],
      total: 21,
      page: 1,
      size: 20,
      pages: 2,
    });
    mockedNotificationService.getUnreadCount.mockResolvedValue({ count: 2 });
    mockedNotificationService.markAllRead.mockResolvedValue(undefined);

    const element = await renderNotifications();

    expect(element.textContent).toContain('2 条未读');
    const markAllButton = Array.from(element.querySelectorAll('button'))
      .find((button) => button.textContent?.includes('全部已读'));
    expect(markAllButton).toBeTruthy();

    await act(async () => {
      markAllButton?.dispatchEvent(new MouseEvent('click', { bubbles: true }));
    });
    await flushAsyncWork();

    expect(mockedNotificationService.markAllRead).toHaveBeenCalledTimes(1);
    expect(element.textContent).not.toContain('2 条未读');
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
