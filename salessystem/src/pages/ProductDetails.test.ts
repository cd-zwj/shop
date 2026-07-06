import React, { act } from 'react';
import { createRoot, type Root } from 'react-dom/client';
import { MemoryRouter, Route, Routes } from 'react-router-dom';
import { afterEach, describe, expect, it, vi } from 'vitest';
import ProductDetails from './ProductDetails';
import { appCatalogService } from '../services/modules/appCatalog';
import type { Product, Tenant } from '../types/catalog';

vi.mock('../context/AuthContext', () => ({
  useAuth: () => ({
    currentRole: 'user',
  }),
}));

vi.mock('../context/CartContext', () => ({
  useCart: () => ({
    addItem: vi.fn(),
    totalItems: 0,
  }),
}));

vi.mock('../services/modules/appCatalog', () => ({
  appCatalogService: {
    getProduct: vi.fn(),
    getTenant: vi.fn(),
  },
}));

const mockedCatalogService = vi.mocked(appCatalogService);

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

async function renderProductDetails() {
  container = document.createElement('div');
  document.body.appendChild(container);
  root = createRoot(container);

  await act(async () => {
    root?.render(
      React.createElement(
        MemoryRouter,
        { initialEntries: ['/product/17'] },
        React.createElement(
          Routes,
          null,
          React.createElement(Route, { path: '/product/:id', element: React.createElement(ProductDetails) }),
        ),
      ),
    );
  });

  await flushAsyncWork();
  return container;
}

describe('ProductDetails', () => {
  it('shows virtual delivery guidance and blocks purchasing inactive products', async () => {
    mockedCatalogService.getProduct.mockResolvedValue(buildProduct({
      id: 17,
      name: '会员兑换码',
      fulfillmentMode: 'ONLINE_VIRTUAL',
      productType: 'CARD_KEY',
      stock: 20,
      status: 0,
    }));
    mockedCatalogService.getTenant.mockResolvedValue(buildTenant());

    const element = await renderProductDetails();

    expect(element.textContent).toContain('会员兑换码');
    expect(element.textContent).toContain('卡密查看位置');
    expect(element.textContent).toContain('我的已购');
    expect(element.textContent).toContain('商品已下架');
    expect(element.textContent).toContain('暂不可购买');

    const purchaseButtons = Array.from(element.querySelectorAll('button'))
      .filter((button) => button.textContent?.includes('加入购物车') || button.textContent?.includes('立即购买'));
    expect(purchaseButtons.length).toBeGreaterThan(0);
    expect(purchaseButtons.every((button) => button.disabled)).toBe(true);
  });
});

function buildProduct(overrides: Partial<Product>): Product {
  return {
    id: 1,
    tenantId: 9,
    productCode: 'SKU-1',
    name: '商品',
    price: 9900,
    unit: '件',
    category: '虚拟商品',
    description: '商品描述',
    imageUrl: null,
    stock: 10,
    fulfillmentMode: 'ONLINE_VIRTUAL',
    productType: 'VIRTUAL',
    status: 1,
    createTime: '2026-07-06T10:00:00',
    ...overrides,
  };
}

function buildTenant(): Tenant {
  return {
    id: 9,
    name: '测试商户',
    contact: '张店长',
    phone: '13800000000',
    address: '本地测试地址',
  };
}
