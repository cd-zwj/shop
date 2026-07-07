import React, { act } from 'react';
import { createRoot, type Root } from 'react-dom/client';
import { MemoryRouter, Route, Routes } from 'react-router-dom';
import { afterEach, describe, expect, it, vi } from 'vitest';
import MerchantRefunds from './MerchantRefunds';
import { merchantRefundService } from '../../services/modules/merchantRefund';
import type { Refund } from '../../types/refund';

const mockShowToast = vi.hoisted(() => vi.fn());

vi.mock('../../context/AuthContext', () => ({
  useAuth: () => ({
    merchantSession: {
      tenantId: 9,
      tenantName: '测试店铺',
      employeeRole: 'OWNER',
    },
  }),
}));

vi.mock('../../context/ToastContext', () => ({
  useToast: () => ({
    showToast: mockShowToast,
  }),
}));

vi.mock('../../services/modules/merchantRefund', () => ({
  merchantRefundService: {
    listRefunds: vi.fn(),
    auditRefund: vi.fn(),
  },
}));

const mockedRefundService = vi.mocked(merchantRefundService);

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

async function renderMerchantRefunds() {
  container = document.createElement('div');
  document.body.appendChild(container);
  root = createRoot(container);

  await act(async () => {
    root?.render(
      React.createElement(
        MemoryRouter,
        { initialEntries: ['/merchant/refunds'] },
        React.createElement(
          Routes,
          null,
          React.createElement(Route, {
            path: '/merchant/refunds',
            element: React.createElement(MerchantRefunds),
          }),
        ),
      ),
    );
  });

  await flushAsyncWork();
  return container;
}

describe('MerchantRefunds', () => {
  it('shows a retryable error state when merchant refunds fail to load', async () => {
    mockedRefundService.listRefunds
      .mockRejectedValueOnce(new Error('售后接口不可用'))
      .mockResolvedValueOnce({
        records: [buildRefund({ refundNo: 'RA202607080001', reason: '商品不合适' })],
        total: 1,
        page: 1,
        size: 50,
        pages: 1,
      });

    const element = await renderMerchantRefunds();

    expect(element.textContent).toContain('售后接口不可用');
    expect(element.textContent).toContain('重试');
    expect(mockedRefundService.listRefunds).toHaveBeenCalledTimes(1);

    const retryButton = Array.from(element.querySelectorAll('button'))
      .find((button) => button.textContent?.includes('重试'));
    expect(retryButton).toBeTruthy();

    await act(async () => {
      retryButton?.dispatchEvent(new MouseEvent('click', { bubbles: true }));
    });
    await flushAsyncWork();

    expect(mockedRefundService.listRefunds).toHaveBeenCalledTimes(2);
    expect(element.textContent).toContain('RA202607080001');
    expect(element.textContent).toContain('商品不合适');
    expect(element.textContent).not.toContain('售后接口不可用');
  });
});

function buildRefund(overrides: Partial<Refund> = {}): Refund {
  return {
    id: 1,
    refundNo: 'RA202607080000',
    orderNo: 'SO202607080000',
    orderItemId: null,
    refundType: 'REFUND_ONLY',
    refundStatus: 'PENDING',
    refundAmount: 128,
    deliveryStatus: 'PENDING',
    refundableAmount: 128,
    quickRefundSuggested: true,
    refundSuggestion: '未发货，商家同意后可快速退款',
    reason: '不想要了',
    description: null,
    rejectReason: null,
    auditTime: null,
    completeTime: null,
    createTime: '2026-07-08T10:00:00',
    ...overrides,
  };
}
