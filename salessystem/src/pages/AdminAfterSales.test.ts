import React, { act } from 'react';
import { createRoot, type Root } from 'react-dom/client';
import { MemoryRouter } from 'react-router-dom';
import { afterEach, describe, expect, it, vi } from 'vitest';
import AdminAfterSales from './AdminAfterSales';
import { adminAfterSaleService } from '../services/modules/adminAfterSale';
import type { AdminAfterSale } from '../types/refund';

const mockShowToast = vi.hoisted(() => vi.fn());
const mockAdminPermissions = vi.hoisted(() => ({
  value: ['admin:after-sale:list', 'admin:after-sale:manage'],
}));

vi.mock('../context/ToastContext', () => ({
  useToast: () => ({ showToast: mockShowToast }),
}));

vi.mock('../context/AuthContext', () => ({
  useAuth: () => ({ adminSession: { permissions: mockAdminPermissions.value } }),
}));

vi.mock('../services/modules/adminAfterSale', () => ({
  adminAfterSaleService: {
    listRefunds: vi.fn(),
    getRefund: vi.fn(),
    listActions: vi.fn(),
    intervene: vi.fn(),
  },
}));

const mockedService = vi.mocked(adminAfterSaleService);
let root: Root | null = null;
let container: HTMLDivElement | null = null;

afterEach(() => {
  if (root) act(() => root?.unmount());
  container?.remove();
  root = null;
  container = null;
  mockAdminPermissions.value = ['admin:after-sale:list', 'admin:after-sale:manage'];
  vi.clearAllMocks();
});

async function flush() {
  await act(async () => {
    await Promise.resolve();
    await Promise.resolve();
    await Promise.resolve();
  });
}

async function renderPage() {
  container = document.createElement('div');
  document.body.appendChild(container);
  root = createRoot(container);
  await act(async () => {
    root?.render(React.createElement(
      MemoryRouter,
      null,
      React.createElement(AdminAfterSales),
    ));
  });
  await flush();
  return container;
}

describe('AdminAfterSales', () => {
  it('shows a retryable error state and recovers the cross-tenant queue', async () => {
    mockedService.listRefunds
      .mockRejectedValueOnce(new Error('平台售后接口不可用'))
      .mockResolvedValueOnce(pageOf(buildAfterSale()));

    const element = await renderPage();
    expect(element.textContent).toContain('平台售后接口不可用');

    await click(findButton(element, '重试'));

    expect(mockedService.listRefunds).toHaveBeenCalledTimes(2);
    expect(element.textContent).toContain('RA-ADMIN-1');
    expect(element.textContent).toContain('租户 9');
    expect(element.textContent).toContain('¥12.80');
  });

  it('applies status, tenant and keyword filters to the platform queue', async () => {
    mockedService.listRefunds.mockResolvedValue(pageOf(buildAfterSale()));
    const element = await renderPage();

    setInputValue(element.querySelector('select'), 'PENDING');
    setInputValue(element.querySelector('input[aria-label="租户 ID"]'), '9');
    setInputValue(element.querySelector('input[aria-label="退款单号或订单号"]'), ' SO-ADMIN-1 ');
    await click(findButton(element, '查询'));

    expect(mockedService.listRefunds).toHaveBeenLastCalledWith({
      status: 'PENDING', tenantId: 9, keyword: 'SO-ADMIN-1', pageNum: 1, pageSize: 20,
    });
  });

  it('loads immutable actions and submits a trimmed optimistic intervention decision', async () => {
    const refund = buildAfterSale();
    mockedService.listRefunds.mockResolvedValue(pageOf(refund));
    mockedService.getRefund.mockResolvedValue(refund);
    mockedService.listActions.mockResolvedValue([
      { action: 'USER_APPLY', operatorRole: 'USER', remark: '包装破损', evidenceUrls: [], createTime: '2026-08-05 10:00:00' },
    ]);
    mockedService.intervene.mockResolvedValue(undefined);

    const element = await renderPage();
    await click(findButton(element, '查看详情'));

    expect(mockedService.getRefund).toHaveBeenCalledWith(9, 1);
    expect(mockedService.listActions).toHaveBeenCalledWith(9, 1);
    expect(element.textContent).toContain('提交申请');
    expect(element.textContent).toContain('¥12.80');

    setInputValue(element.querySelector('textarea'), '  核对订单与凭证后同意退款  ');
    await click(findButton(element, '确认同意退款'));

    expect(mockedService.intervene).toHaveBeenCalledWith(
      9, 1, 'PENDING', true, '核对订单与凭证后同意退款',
    );
    expect(mockShowToast).toHaveBeenCalledWith('平台售后决定已提交', 'success');
  });

  it('requires a remark no longer than 1000 characters', async () => {
    await openRefundDetail();
    const textarea = container?.querySelector('textarea') ?? null;

    await click(findButton(container!, '确认同意退款'));
    expect(mockShowToast).toHaveBeenCalledWith('请填写平台处理说明', 'error');
    expect(mockedService.intervene).not.toHaveBeenCalled();

    setInputValue(textarea, 'a'.repeat(1001));
    await click(findButton(container!, '确认同意退款'));
    expect(mockShowToast).toHaveBeenCalledWith('平台处理说明不能超过 1000 个字符', 'error');
    expect(mockedService.intervene).not.toHaveBeenCalled();
  });

  it('prevents duplicate submissions and keeps the remark after a failed decision', async () => {
    await openRefundDetail();
    const textarea = container?.querySelector('textarea') ?? null;
    setInputValue(textarea, '需要再次核验凭证');

    let rejectRequest: ((reason: Error) => void) | undefined;
    mockedService.intervene.mockImplementation(() => new Promise<void>((_resolve, reject) => {
      rejectRequest = reject;
    }));
    const submit = findButton(container!, '确认同意退款');
    await act(async () => {
      submit.click();
      submit.click();
      await Promise.resolve();
    });

    expect(mockedService.intervene).toHaveBeenCalledTimes(1);
    expect(submit.disabled).toBe(true);

    await act(async () => {
      rejectRequest?.(new Error('售后状态已变更，请刷新后重试'));
      await Promise.resolve();
      await Promise.resolve();
    });

    expect(mockShowToast).toHaveBeenCalledWith('售后状态已变更，请刷新后重试', 'error');
    expect((textarea as HTMLTextAreaElement).value).toBe('需要再次核验凭证');
    expect(submit.disabled).toBe(false);
  });

  it('only allows approval when reconsidering an already rejected request', async () => {
    const refund = buildAfterSale({ refundStatus: 'REJECTED', statusLabel: '已驳回' });
    mockedService.listRefunds.mockResolvedValue(pageOf(refund));
    mockedService.getRefund.mockResolvedValue(refund);
    mockedService.listActions.mockResolvedValue([]);

    const element = await renderPage();
    await click(findButton(element, '查看详情'));

    expect(element.textContent).toContain('已驳回售后仅可由平台重新同意退款');
    expect(Array.from(element.querySelectorAll('button'))
      .some((button) => button.textContent?.includes('驳回申请'))).toBe(false);
  });

  it('keeps the newest list result when filter requests resolve out of order', async () => {
    const first = deferred<ReturnType<typeof pageOf>>();
    const second = deferred<ReturnType<typeof pageOf>>();
    mockedService.listRefunds
      .mockReturnValueOnce(first.promise)
      .mockReturnValueOnce(second.promise);

    const element = await renderPage();
    setInputValue(element.querySelector('input[aria-label="租户 ID"]'), '10');
    await click(findButton(element, '查询'));

    await act(async () => {
      second.resolve(pageOf(buildAfterSale({ id: 2, tenantId: 10, refundNo: 'RA-NEWEST' })));
      await Promise.resolve();
      await Promise.resolve();
    });
    await act(async () => {
      first.resolve(pageOf(buildAfterSale({ refundNo: 'RA-STALE' })));
      await Promise.resolve();
      await Promise.resolve();
    });

    expect(element.textContent).toContain('RA-NEWEST');
    expect(element.textContent).not.toContain('RA-STALE');
  });

  it('keeps the newest detail when two detail requests resolve out of order', async () => {
    const firstRefund = buildAfterSale();
    const secondRefund = buildAfterSale({ id: 2, tenantId: 10, refundNo: 'RA-ADMIN-2' });
    const firstDetail = deferred<AdminAfterSale>();
    const secondDetail = deferred<AdminAfterSale>();
    const firstActions = deferred<[]>();
    const secondActions = deferred<[]>();
    mockedService.listRefunds.mockResolvedValue({
      records: [firstRefund, secondRefund], total: 2, page: 1, current: 1, size: 20, pages: 1,
    });
    mockedService.getRefund
      .mockReturnValueOnce(firstDetail.promise)
      .mockReturnValueOnce(secondDetail.promise);
    mockedService.listActions
      .mockReturnValueOnce(firstActions.promise)
      .mockReturnValueOnce(secondActions.promise);

    const element = await renderPage();
    const detailButtons = Array.from(element.querySelectorAll('button'))
      .filter((button) => button.textContent?.includes('查看详情'));
    await click(detailButtons[0]);
    await click(detailButtons[1]);

    await act(async () => {
      secondDetail.resolve(secondRefund);
      secondActions.resolve([]);
      await Promise.resolve();
      await Promise.resolve();
    });
    await act(async () => {
      firstDetail.resolve(firstRefund);
      firstActions.resolve([]);
      await Promise.resolve();
      await Promise.resolve();
    });

    expect(element.querySelector('[role="dialog"]')?.textContent).toContain('RA-ADMIN-2');
    expect(element.querySelector('[role="dialog"]')?.textContent).not.toContain('RA-ADMIN-1');
  });

  it('renders read-only detail without intervention controls for list-only admins', async () => {
    mockAdminPermissions.value = ['admin:after-sale:list'];
    await openRefundDetail();

    expect(container?.textContent).toContain('当前账号没有平台售后处理权限');
    expect(container?.querySelector('textarea')).toBeNull();
    expect(Array.from(container?.querySelectorAll('button') ?? [])
      .some((button) => button.textContent?.includes('确认同意退款'))).toBe(false);
  });
});

async function openRefundDetail() {
  const refund = buildAfterSale();
  mockedService.listRefunds.mockResolvedValue(pageOf(refund));
  mockedService.getRefund.mockResolvedValue(refund);
  mockedService.listActions.mockResolvedValue([]);
  await renderPage();
  await click(findButton(container!, '查看详情'));
}

async function click(button: HTMLButtonElement) {
  await act(async () => {
    button.click();
    await Promise.resolve();
    await Promise.resolve();
  });
}

function setInputValue(element: Element | null, value: string) {
  if (!element) throw new Error('form control not found');
  const prototype = element instanceof HTMLSelectElement
    ? HTMLSelectElement.prototype
    : element instanceof HTMLTextAreaElement
      ? HTMLTextAreaElement.prototype
      : HTMLInputElement.prototype;
  Object.getOwnPropertyDescriptor(prototype, 'value')?.set?.call(element, value);
  element.dispatchEvent(new Event('change', { bubbles: true }));
  element.dispatchEvent(new Event('input', { bubbles: true }));
}

function findButton(element: HTMLElement, label: string) {
  const button = Array.from(element.querySelectorAll('button'))
    .find((candidate) => candidate.textContent?.includes(label));
  if (!button) throw new Error(`button not found: ${label}`);
  return button;
}

function pageOf(refund: AdminAfterSale) {
  return { records: [refund], total: 1, page: 1, current: 1, size: 20, pages: 1 };
}

function buildAfterSale(overrides: Partial<AdminAfterSale> = {}): AdminAfterSale {
  return {
    id: 1,
    tenantId: 9,
    refundNo: 'RA-ADMIN-1',
    orderNo: 'SO-ADMIN-1',
    orderItemId: null,
    refundType: 'REFUND_ONLY',
    refundStatus: 'PENDING',
    refundAmount: 1280,
    deliveryStatus: 'PENDING',
    refundableAmount: 1280,
    quickRefundSuggested: true,
    refundSuggestion: '可快速退款',
    statusLabel: '待审核',
    statusDescription: '等待商户或平台审核',
    nextStep: '核对订单与凭证',
    failureReason: null,
    availableActions: ['ADMIN_INTERVENE'],
    reason: '包装破损',
    description: '到店检查时发现外包装破损',
    evidenceUrls: ['https://example.test/evidence.jpg'],
    rejectReason: null,
    auditTime: null,
    completeTime: null,
    createTime: '2026-08-05 10:00:00',
    updateTime: '2026-08-05 10:00:00',
    ...overrides,
  };
}

function deferred<T>() {
  let resolve!: (value: T) => void;
  const promise = new Promise<T>((promiseResolve) => {
    resolve = promiseResolve;
  });
  return { promise, resolve };
}
