import React, { act } from 'react';
import { createRoot, type Root } from 'react-dom/client';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { AuthProvider, useAuth } from './AuthContext';
import { AUTH_TOKEN_CLEAR_EVENT } from '../services/http';
import type { PlatformLoginDTO } from '../types/auth';

const mockLoginByPassword = vi.fn();
const mockGetCurrentUser = vi.fn();
const mockAdminLogin = vi.fn();
const mockGetCurrentSession = vi.fn();
const mockGetMerchantSession = vi.fn();

vi.mock('../services/modules/appAuth', () => ({
  appAuthService: {
    loginByPassword: (...args: unknown[]) => mockLoginByPassword(...args),
    loginBySms: vi.fn(),
    loginByThirdParty: vi.fn(),
    register: vi.fn(),
    logout: vi.fn(),
  },
}));

vi.mock('../services/modules/appUser', () => ({
  appUserService: {
    getCurrentUser: (...args: unknown[]) => mockGetCurrentUser(...args),
  },
}));

vi.mock('../services/modules/adminAuth', () => ({
  adminAuthService: {
    login: (...args: unknown[]) => mockAdminLogin(...args),
    getCurrentSession: (...args: unknown[]) => mockGetCurrentSession(...args),
    logout: vi.fn(),
  },
}));

vi.mock('../services/modules/merchantAuth', () => ({
  merchantAuthService: {
    login: vi.fn(),
    getCurrentSession: (...args: unknown[]) => mockGetMerchantSession(...args),
    logout: vi.fn(),
  },
}));

type AuthContextShape = ReturnType<typeof useAuth>;

function Probe({ onValue }: { onValue: (value: AuthContextShape) => void }) {
  onValue(useAuth());
  return null;
}

function renderProvider(onValue: (value: AuthContextShape) => void) {
  const container = document.createElement('div');
  document.body.appendChild(container);
  const root = createRoot(container);
  act(() => {
    root.render(
      React.createElement(
        AuthProvider,
        null,
        React.createElement(Probe, { onValue }),
      ),
    );
  });
  return { root, container };
}

function cleanup(root: Root, container: HTMLDivElement) {
  act(() => {
    root.unmount();
  });
  container.remove();
}

describe('AuthProvider', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    window.localStorage.clear();
  });

  it('有当前角色但没有对应 token 时，不再请求当前用户并清理残留角色', async () => {
    window.localStorage.setItem('salessystem:current-role', 'user');

    let auth: AuthContextShape | null = null;
    const { root, container } = renderProvider((value) => {
      auth = value;
    });

    await act(async () => {
      await Promise.resolve();
    });

    expect(mockGetCurrentUser).not.toHaveBeenCalled();
    expect(window.localStorage.getItem('salessystem:current-role')).toBeNull();
    expect(auth?.currentRole).toBeNull();
    cleanup(root, container);
  });

  it('登录过程中收到旧的 401 清理事件时，保留刚写入的用户 token 和会话', async () => {
    const payload: PlatformLoginDTO = {
      username: 'sleephhh',
      password: 'correct-password',
      captchaKey: 'captcha-key',
      captchaCode: '1234',
    };
    const profile = { id: 7, username: 'sleephhh' };
    mockLoginByPassword.mockResolvedValue('fresh-user-token');
    mockGetCurrentUser.mockImplementation(async () => {
      window.dispatchEvent(new CustomEvent(AUTH_TOKEN_CLEAR_EVENT, { detail: { role: 'user' } }));
      return profile;
    });

    let auth: AuthContextShape | null = null;
    const { root, container } = renderProvider((value) => {
      auth = value;
    });

    await act(async () => {
      await Promise.resolve();
    });

    await act(async () => {
      await auth?.loginUser('password', payload);
    });

    expect(window.localStorage.getItem('salessystem:app:token')).toBe('fresh-user-token');
    expect(window.localStorage.getItem('salessystem:current-role')).toBe('user');
    expect(JSON.parse(window.localStorage.getItem('salessystem:user:profile') ?? '{}')).toEqual(profile);
    expect(auth?.currentUser).toEqual(profile);
    cleanup(root, container);
  });

  it('管理员登录后会把 token、角色和 session 写入登录态', async () => {
    const payload: PlatformLoginDTO = {
      username: 'admin',
      password: 'correct-password',
      captchaKey: 'captcha-key',
      captchaCode: '1234',
    };
    const session = {
      userId: 1,
      username: 'admin',
      role: 'admin',
      scope: 'all',
      permissions: [],
      roles: ['admin'],
    };
    mockAdminLogin.mockResolvedValue('fresh-admin-token');
    mockGetCurrentSession.mockResolvedValue(session);

    let auth: AuthContextShape | null = null;
    const { root, container } = renderProvider((value) => {
      auth = value;
    });

    await act(async () => {
      await Promise.resolve();
    });

    await act(async () => {
      await auth?.loginAdmin(payload);
    });

    expect(window.localStorage.getItem('salessystem:platform:token')).toBe('fresh-admin-token');
    expect(window.localStorage.getItem('salessystem:current-role')).toBe('admin');
    expect(JSON.parse(window.localStorage.getItem('salessystem:admin:session') ?? '{}')).toEqual(session);
    expect(auth?.adminSession).toEqual(session);
    expect(auth?.currentRole).toBe('admin');
    cleanup(root, container);
  });
});
