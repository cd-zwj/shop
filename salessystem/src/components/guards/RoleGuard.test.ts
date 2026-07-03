import React from 'react';
import { createRoot, type Root } from 'react-dom/client';
import { MemoryRouter } from 'react-router-dom';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import RoleGuard from './RoleGuard';

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
      React.createElement(RoleGuard, {
        allowedRoles: ['admin'],
        children: React.createElement('div', null, 'admin content'),
      }),
    ),
  );

  return { container, root };
}

function cleanup(root: Root, container: HTMLDivElement) {
  root.unmount();
  container.remove();
}

describe('RoleGuard', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    window.localStorage.clear();
  });

  afterEach(() => {
    window.localStorage.clear();
  });

  it('本地已有管理员 session 时，不因上下文首帧为空而跳转', async () => {
    window.localStorage.setItem('salessystem:current-role', 'admin');
    window.localStorage.setItem(
      'salessystem:admin:session',
      JSON.stringify({
        userId: 1,
        username: 'admin',
        role: 'admin',
        scope: 'all',
        permissions: [],
        roles: ['admin'],
      }),
    );

    mockUseAuth.mockReturnValue({
      currentRole: null,
      currentUser: null,
      merchantSession: null,
      adminSession: null,
    });

    const { root, container } = renderGuard();

    await vi.waitFor(() => {
      expect(container.textContent).toContain('admin content');
    });

    cleanup(root, container);
  });
});
