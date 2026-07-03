import React from 'react';
import { createRoot, type Root } from 'react-dom/client';
import { MemoryRouter } from 'react-router-dom';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import AuthGuard from './AuthGuard';
import type { AuthRole } from '../../types/auth';

const mockUseAuth = vi.fn();

vi.mock('../../context/AuthContext', () => ({
  useAuth: () => mockUseAuth(),
}));

function renderGuard() {
  const container = document.createElement('div');
  document.body.appendChild(container);
  const root = createRoot(container);

  root.render(
    React.createElement(
      MemoryRouter,
      { initialEntries: ['/admin'] },
      React.createElement(
        AuthGuard,
        null,
        React.createElement('div', null, 'protected content'),
      ),
    ),
  );

  return { container, root };
}

function cleanup(root: Root, container: HTMLDivElement) {
  root.unmount();
  container.remove();
}

function setStoredSession(role: AuthRole, session: unknown) {
  const sessionKey =
    role === 'admin'
      ? 'salessystem:admin:session'
      : role === 'merchant'
        ? 'salessystem:merchant:session'
        : 'salessystem:user:profile';

  window.localStorage.setItem('salessystem:current-role', role);
  window.localStorage.setItem(sessionKey, JSON.stringify(session));
}

describe('AuthGuard', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    window.localStorage.clear();
  });

  afterEach(() => {
    window.localStorage.clear();
  });

  it('本地已有刚写入的管理员 session 时，不因上下文首帧为空而跳回登录页', async () => {
    window.localStorage.setItem('salessystem:platform:token', 'fresh-admin-token');
    setStoredSession('admin', {
      userId: 1,
      username: 'admin',
      role: 'admin',
      scope: 'all',
      permissions: [],
      roles: ['admin'],
    });

    mockUseAuth.mockReturnValue({
      isReady: true,
      currentRole: null,
      currentUser: null,
      merchantSession: null,
      adminSession: null,
      refreshCurrentUser: vi.fn(),
      refreshMerchantSession: vi.fn(),
      refreshAdminSession: vi.fn(),
      logout: vi.fn(),
    });

    const { root, container } = renderGuard();

    await vi.waitFor(() => {
      expect(container.textContent).toContain('protected content');
    });

    cleanup(root, container);
  });
});
