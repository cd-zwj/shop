import React, { act } from 'react';
import { createRoot, type Root } from 'react-dom/client';
import { MemoryRouter, Route, Routes } from 'react-router-dom';
import { afterEach, describe, expect, it, vi } from 'vitest';
import { TopNav } from './TopNav';
import { appNotificationService } from '../../services/modules/appNotification';
import type { AuthRole } from '../../types/auth';

const mockLogout = vi.fn();
const mockUseAuth = vi.fn();

vi.mock('../../context/AuthContext', () => ({
  useAuth: () => mockUseAuth(),
}));

vi.mock('../../services/modules/appNotification', () => ({
  appNotificationService: {
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

async function renderTopNav(currentRole: AuthRole | null = 'user') {
  mockUseAuth.mockReturnValue({
    currentRole,
    logout: mockLogout,
  });
  container = document.createElement('div');
  document.body.appendChild(container);
  root = createRoot(container);

  await act(async () => {
    root?.render(
      React.createElement(
        MemoryRouter,
        { initialEntries: ['/'] },
        React.createElement(
          Routes,
          null,
          React.createElement(Route, {
            path: '/',
            element: React.createElement(TopNav, { title: 'SalesSystem' }),
          }),
          React.createElement(Route, {
            path: '/notifications',
            element: React.createElement('div', null, '通知中心页面'),
          }),
        ),
      ),
    );
  });

  await flushAsyncWork();
  return container;
}

describe('TopNav', () => {
  it('shows the unread notification count for logged-in users', async () => {
    mockedNotificationService.getUnreadCount.mockResolvedValue({ count: 7 });

    const element = await renderTopNav('user');

    expect(mockedNotificationService.getUnreadCount).toHaveBeenCalledTimes(1);
    expect(element.querySelector('[data-testid="top-nav-notification-count"]')?.textContent).toBe('7');
  });

  it('hides the notification badge when the unread count is zero', async () => {
    mockedNotificationService.getUnreadCount.mockResolvedValue({ count: 0 });

    const element = await renderTopNav('user');

    expect(mockedNotificationService.getUnreadCount).toHaveBeenCalledTimes(1);
    expect(element.querySelector('[data-testid="top-nav-notification-count"]')).toBeNull();
  });

  it('hides the notification badge when the unread count request fails', async () => {
    mockedNotificationService.getUnreadCount.mockRejectedValue(new Error('unauthorized'));

    const element = await renderTopNav('user');

    expect(mockedNotificationService.getUnreadCount).toHaveBeenCalledTimes(1);
    expect(element.querySelector('[data-testid="top-nav-notification-count"]')).toBeNull();
  });

  it('does not request user notifications outside the user role', async () => {
    const element = await renderTopNav('merchant');

    expect(mockedNotificationService.getUnreadCount).not.toHaveBeenCalled();
    expect(element.querySelector('[data-testid="top-nav-notification-count"]')).toBeNull();
  });

  it('navigates to the notification center from the bell button', async () => {
    mockedNotificationService.getUnreadCount.mockResolvedValue({ count: 1 });

    const element = await renderTopNav('user');
    const button = element.querySelector('button[aria-label="消息通知，1 条未读"]');
    expect(button).toBeTruthy();

    await act(async () => {
      button?.dispatchEvent(new MouseEvent('click', { bubbles: true }));
    });
    await flushAsyncWork();

    expect(element.textContent).toContain('通知中心页面');
  });
});
