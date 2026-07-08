import React, { act } from 'react';
import { createRoot, type Root } from 'react-dom/client';
import { MemoryRouter, Route, Routes } from 'react-router-dom';
import { afterEach, describe, expect, it, vi } from 'vitest';
import MerchantTasks from './MerchantTasks';
import { merchantWorkbenchService } from '../../services/modules/merchantWorkbench';

vi.mock('../../context/AuthContext', () => ({
  useAuth: () => ({
    merchantSession: {
      tenantId: 9,
      tenantName: '测试店铺',
      employeeRole: 'OWNER',
    },
  }),
}));

vi.mock('../../services/modules/merchantWorkbench', () => ({
  merchantWorkbenchService: {
    listTasks: vi.fn(),
  },
}));

const mockedWorkbenchService = vi.mocked(merchantWorkbenchService);

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

async function renderMerchantTasks(path = '/merchant/tasks?type=compensation') {
  container = document.createElement('div');
  document.body.appendChild(container);
  root = createRoot(container);

  await act(async () => {
    root?.render(
      React.createElement(
        MemoryRouter,
        { initialEntries: [path] },
        React.createElement(
          Routes,
          null,
          React.createElement(Route, { path: '/merchant/tasks', element: React.createElement(MerchantTasks) }),
        ),
      ),
    );
  });

  await flushAsyncWork();
  return container;
}

describe('MerchantTasks', () => {
  it('shows merchant visible compensation task details and follow-up action', async () => {
    mockedWorkbenchService.listTasks.mockResolvedValue({
      records: [{
        taskSource: 'compensation',
        id: 11,
        taskNo: 'CT202607080001',
        taskType: 'MERCHANT_APPROVED_REFUND',
        bizType: 'MERCHANT_APPROVED_REFUND',
        bizNo: 'RA202607080001',
        taskStatus: 'FAIL',
        retryCount: 5,
        maxRetryCount: null,
        nextRetryTime: null,
        lastError: 'Provider refund is not supported in phase 1',
        createTime: '2026-07-08T10:00:00',
        updateTime: '2026-07-08T10:05:00',
        actionLabel: '查看退款单',
        actionPath: '/merchant/refunds?status=FAILED',
      }],
      total: 1,
      page: 1,
      current: 1,
      size: 20,
      pages: 1,
    });

    const element = await renderMerchantTasks();

    expect(mockedWorkbenchService.listTasks).toHaveBeenCalledWith(9, {
      type: 'compensation',
      pageNum: 1,
      pageSize: 20,
    });
    expect(element.textContent).toContain('系统任务跟进');
    expect(element.textContent).toContain('CT202607080001');
    expect(element.textContent).toContain('RA202607080001');
    expect(element.textContent).toContain('Provider refund is not supported in phase 1');
    expect(element.textContent).toContain('查看退款单');
  });

  it('switches to retry tasks from query string', async () => {
    mockedWorkbenchService.listTasks.mockResolvedValue({
      records: [],
      total: 0,
      page: 1,
      current: 1,
      size: 20,
      pages: 0,
    });

    const element = await renderMerchantTasks('/merchant/tasks?type=retry');

    expect(mockedWorkbenchService.listTasks).toHaveBeenCalledWith(9, {
      type: 'retry',
      pageNum: 1,
      pageSize: 20,
    });
    expect(element.textContent).toContain('暂无系统任务');
  });

  it('shows a retryable error state when task list fails to load', async () => {
    mockedWorkbenchService.listTasks
      .mockRejectedValueOnce(new Error('任务接口不可用'))
      .mockResolvedValueOnce({
        records: [],
        total: 0,
        page: 1,
        current: 1,
        size: 20,
        pages: 0,
      });

    const element = await renderMerchantTasks();

    expect(element.textContent).toContain('任务接口不可用');
    const retryButton = Array.from(element.querySelectorAll('button'))
      .find((button) => button.textContent?.includes('重试'));
    expect(retryButton).toBeTruthy();

    await act(async () => {
      retryButton?.dispatchEvent(new MouseEvent('click', { bubbles: true }));
    });
    await flushAsyncWork();

    expect(mockedWorkbenchService.listTasks).toHaveBeenCalledTimes(2);
    expect(element.textContent).toContain('暂无系统任务');
  });
});
