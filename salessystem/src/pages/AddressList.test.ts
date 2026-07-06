import React, { act } from 'react';
import { createRoot, type Root } from 'react-dom/client';
import { MemoryRouter, Route, Routes } from 'react-router-dom';
import { afterEach, describe, expect, it, vi } from 'vitest';
import AddressList from './AddressList';
import { ToastProvider } from '../context/ToastContext';
import { appAddressService } from '../services/modules/appAddress';
import type { Address } from '../types/addressNotification';

vi.mock('motion/react', () => ({
  AnimatePresence: ({ children }: { children: React.ReactNode }) => children,
  motion: new Proxy({}, {
    get: (_target, tag: string) => tag,
  }),
}));

vi.mock('../services/modules/appAddress', () => ({
  appAddressService: {
    list: vi.fn(),
    create: vi.fn(),
    update: vi.fn(),
    remove: vi.fn(),
    setDefault: vi.fn(),
  },
}));

const mockedAddressService = vi.mocked(appAddressService);

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
    await Promise.resolve();
  });
}

async function renderAddressList() {
  container = document.createElement('div');
  document.body.appendChild(container);
  root = createRoot(container);

  await act(async () => {
    root?.render(
      React.createElement(
        MemoryRouter,
        { initialEntries: ['/addresses'] },
        React.createElement(
          ToastProvider,
          null,
          React.createElement(
            Routes,
            null,
            React.createElement(Route, { path: '/addresses', element: React.createElement(AddressList) }),
          ),
        ),
      ),
    );
  });

  await flushAsyncWork();
  return container;
}

describe('AddressList', () => {
  it('shows a retryable error state when addresses fail to load', async () => {
    mockedAddressService.list
      .mockRejectedValueOnce(new Error('地址服务不可用'))
      .mockResolvedValueOnce([buildAddress({
        id: 12,
        receiverName: '王小明',
        phone: '13800000000',
        city: '杭州',
        detail: '未来科技城 1 号',
      })]);

    const element = await renderAddressList();

    expect(element.textContent).toContain('地址服务不可用');
    expect(element.textContent).toContain('重试');
    expect(element.textContent).not.toContain('暂无收货地址');

    const retryButton = Array.from(element.querySelectorAll('button'))
      .find((button) => button.textContent?.includes('重试'));
    expect(retryButton).toBeTruthy();

    await act(async () => {
      retryButton?.dispatchEvent(new MouseEvent('click', { bubbles: true }));
    });
    await flushAsyncWork();

    expect(element.textContent).toContain('王小明');
    expect(element.textContent).toContain('未来科技城 1 号');
    expect(Array.from(element.querySelectorAll('button'))
      .some((button) => button.textContent?.includes('重试'))).toBe(false);
  });
});

function buildAddress(overrides: Partial<Address>): Address {
  return {
    id: 1,
    platformUserId: 2,
    receiverName: '收货人',
    phone: '13900000000',
    province: '浙江省',
    city: '杭州市',
    district: '西湖区',
    detail: '文三路 1 号',
    isDefault: 0,
    deleted: 0,
    createTime: '2026-07-06T10:00:00',
    updateTime: null,
    ...overrides,
  };
}
