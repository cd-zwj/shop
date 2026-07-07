import React, { act } from 'react';
import { createRoot, type Root } from 'react-dom/client';
import { MemoryRouter, Route, Routes } from 'react-router-dom';
import { afterEach, describe, expect, it, vi } from 'vitest';
import MerchantDashboard from './MerchantDashboard';
import { merchantOrderService } from '../../services/modules/merchantOrder';
import { merchantProductService } from '../../services/modules/merchantProduct';
import { merchantRefundService } from '../../services/modules/merchantRefund';
import { merchantWorkbenchService } from '../../services/modules/merchantWorkbench';

vi.mock('recharts', () => ({
  Area: () => React.createElement('div'),
  AreaChart: ({ children }: { children?: React.ReactNode }) => React.createElement('div', null, children),
  CartesianGrid: () => React.createElement('div'),
  ResponsiveContainer: ({ children }: { children?: React.ReactNode }) => React.createElement('div', null, children),
  Tooltip: () => React.createElement('div'),
  XAxis: () => React.createElement('div'),
  YAxis: () => React.createElement('div'),
}));

vi.mock('../../context/AuthContext', () => ({
  useAuth: () => ({
    merchantSession: {
      tenantId: 9,
      tenantName: '测试店铺',
      employeeRole: 'OWNER',
    },
  }),
}));

vi.mock('../../services/modules/merchantOrder', () => ({
  merchantOrderService: {
    listOrders: vi.fn(),
  },
}));

vi.mock('../../services/modules/merchantProduct', () => ({
  merchantProductService: {
    listProducts: vi.fn(),
  },
}));

vi.mock('../../services/modules/merchantRefund', () => ({
  merchantRefundService: {
    listRefunds: vi.fn(),
  },
}));

vi.mock('../../services/modules/merchantWorkbench', () => ({
  merchantWorkbenchService: {
    getTodoSummary: vi.fn(),
  },
}));

const mockedOrderService = vi.mocked(merchantOrderService);
const mockedProductService = vi.mocked(merchantProductService);
const mockedRefundService = vi.mocked(merchantRefundService);
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

async function renderDashboard() {
  container = document.createElement('div');
  document.body.appendChild(container);
  root = createRoot(container);

  await act(async () => {
    root?.render(
      React.createElement(
        MemoryRouter,
        { initialEntries: ['/merchant'] },
        React.createElement(
          Routes,
          null,
          React.createElement(Route, { path: '/merchant', element: React.createElement(MerchantDashboard) }),
        ),
      ),
    );
  });

  await flushAsyncWork();
  return container;
}

describe('MerchantDashboard', () => {
  it('prefers backend todo summary for merchant work items', async () => {
    mockedProductService.listProducts.mockResolvedValue({ records: [], total: 0, page: 1, size: 50, pages: 0 });
    mockedOrderService.listOrders.mockResolvedValue({ records: [], total: 0, page: 1, size: 50, pages: 0 });
    mockedRefundService.listRefunds.mockResolvedValue({ records: [], total: 0, page: 1, size: 50, pages: 0 });
    mockedWorkbenchService.getTodoSummary.mockResolvedValue({
      totalCount: 12,
      items: [
        {
          key: 'fulfillment',
          label: '待履约订单',
          description: '真实后端汇总',
          count: 12,
          path: '/merchant/orders?tab=shipping',
          tone: 'blue',
        },
      ],
    });

    const element = await renderDashboard();

    expect(mockedWorkbenchService.getTodoSummary).toHaveBeenCalledWith(9);
    expect(element.textContent).toContain('12 项');
    expect(element.textContent).toContain('12 个待履约订单');
    expect(element.textContent).toContain('真实后端汇总');
  });

  it('falls back to local todo estimation when backend todo summary fails', async () => {
    mockedProductService.listProducts.mockResolvedValue({
      records: [{ id: 1, tenantId: 9, productCode: 'P1', name: '低库存商品', price: 100, stock: 2, status: 'active' }],
      total: 1,
      page: 1,
      size: 50,
      pages: 1,
    });
    mockedOrderService.listOrders.mockResolvedValue({
      records: [{ id: 1, orderNo: 'SO1', tenantId: 9, platformUserId: 88, orderStatus: 'PAID', payStatus: 'SUCCESS', totalAmount: 100 }],
      total: 1,
      page: 1,
      size: 50,
      pages: 1,
    });
    mockedRefundService.listRefunds.mockResolvedValue({ records: [], total: 0, page: 1, size: 50, pages: 0 });
    mockedWorkbenchService.getTodoSummary.mockRejectedValue(new Error('待办接口暂不可用'));

    const element = await renderDashboard();

    expect(element.textContent).toContain('2 项');
    expect(element.textContent).toContain('1 个待履约订单');
    expect(element.textContent).toContain('1 个低库存商品');
  });
});
