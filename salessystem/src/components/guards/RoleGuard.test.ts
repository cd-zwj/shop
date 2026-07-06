import React from 'react';
import { createRoot, type Root } from 'react-dom/client';
import { MemoryRouter } from 'react-router-dom';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import RoleGuard from './RoleGuard';

const mockUseAuth = vi.fn();

vi.mock('../../context/AuthContext', () => ({
  useAuth: () => mockUseAuth(),
}));

function renderGuard({
  initialPath = '/admin',
  allowedRoles = ['admin'],
  merchantPermission,
}: {
  initialPath?: string;
  allowedRoles?: Array<'user' | 'merchant' | 'admin'>;
  merchantPermission?: import('../../utils/merchantPermissions').MerchantPermission;
} = {}) {
  const container = document.createElement('div');
  document.body.appendChild(container);
  const root = createRoot(container);

  root.render(
    React.createElement(
      MemoryRouter,
      { initialEntries: [initialPath] },
      React.createElement(RoleGuard, {
        allowedRoles,
        merchantPermission,
        children: React.createElement('div', null, 'protected content'),
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

  it('does not trust a cached admin session when context has no server-confirmed session', async () => {
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
      currentRole: 'admin',
      currentUser: null,
      merchantSession: null,
      adminSession: null,
    });

    const { root, container } = renderGuard();

    await vi.waitFor(() => {
      expect(container.textContent).not.toContain('protected content');
    });

    cleanup(root, container);
  });

  it('does not use forged merchant localStorage permissions', async () => {
    window.localStorage.setItem('salessystem:current-role', 'merchant');
    window.localStorage.setItem(
      'salessystem:merchant:session',
      JSON.stringify({
        employeeRole: 'OWNER',
      }),
    );

    mockUseAuth.mockReturnValue({
      currentRole: 'merchant',
      currentUser: null,
      merchantSession: null,
      adminSession: null,
    });

    const { root, container } = renderGuard({
      initialPath: '/merchant/products',
      allowedRoles: ['merchant'],
      merchantPermission: 'product:manage',
    });

    await vi.waitFor(() => {
      expect(container.textContent).not.toContain('protected content');
    });

    cleanup(root, container);
  });

  it('财务员工访问商品模块时会被商户权限拦截', async () => {
    const merchantSession = {
      token: 'token',
      expiresIn: 3600,
      platformUserId: 1,
      username: 'finance',
      tenantId: 2,
      tenantName: '测试商户',
      employeeRole: 'FINANCE',
      tenants: [],
    };

    mockUseAuth.mockReturnValue({
      currentRole: 'merchant',
      currentUser: null,
      merchantSession,
      adminSession: null,
    });

    const { root, container } = renderGuard({
      initialPath: '/merchant/products',
      allowedRoles: ['merchant'],
      merchantPermission: 'product:manage',
    });

    await vi.waitFor(() => {
      expect(container.textContent).not.toContain('protected content');
    });

    cleanup(root, container);
  });
});
