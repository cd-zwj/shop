import React, { act } from 'react';
import { createRoot, type Root } from 'react-dom/client';
import { MemoryRouter, Route, Routes } from 'react-router-dom';
import { afterEach, describe, expect, it, vi } from 'vitest';
import MerchantProductEdit from './MerchantProductEdit';
import { merchantProductService } from '../../services/modules/merchantProduct';
import { merchantProductTaxonomyService } from '../../services/modules/merchantProductTaxonomy';
import { merchantStoreService } from '../../services/modules/merchantStore';
import type { MerchantProduct } from '../../types/merchant';

vi.mock('../../context/AuthContext', () => ({
  useAuth: () => ({
    merchantSession: {
      tenantId: 9,
      tenantName: '测试店铺',
      employeeRole: 'OWNER',
    },
  }),
}));

vi.mock('../../services/modules/fileUpload', () => ({
  fileUploadService: {
    uploadFile: vi.fn(),
  },
}));

vi.mock('../../services/modules/merchantProduct', () => ({
  merchantProductService: {
    getProduct: vi.fn(),
    createProduct: vi.fn(),
    updateProduct: vi.fn(),
    listCardKeys: vi.fn(),
    getCardKeySummary: vi.fn(),
    uploadCardKeys: vi.fn(),
  },
}));

vi.mock('../../services/modules/merchantProductTaxonomy', () => ({
  merchantProductTaxonomyService: {
    listTypes: vi.fn(),
    listCategories: vi.fn(),
  },
}));

vi.mock('../../services/modules/merchantStore', () => ({
  merchantStoreService: {
    listStores: vi.fn(),
  },
}));

const mockedProductService = vi.mocked(merchantProductService);
const mockedTaxonomyService = vi.mocked(merchantProductTaxonomyService);
const mockedStoreService = vi.mocked(merchantStoreService);

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

async function renderMerchantProductEdit() {
  container = document.createElement('div');
  document.body.appendChild(container);
  root = createRoot(container);

  mockedProductService.getProduct.mockResolvedValue(buildProduct());
  mockedStoreService.listStores.mockResolvedValue({ records: [], total: 0, page: 1, size: 100, pages: 0 });
  mockedTaxonomyService.listTypes.mockResolvedValue([]);
  mockedTaxonomyService.listCategories.mockResolvedValue([]);

  await act(async () => {
    root?.render(
      React.createElement(
        MemoryRouter,
        { initialEntries: ['/merchant/product/edit/42'] },
        React.createElement(
          Routes,
          null,
          React.createElement(Route, {
            path: '/merchant/product/edit/:id',
            element: React.createElement(MerchantProductEdit),
          }),
          React.createElement(Route, {
            path: '/product/:id',
            element: React.createElement('div', null, '用户商品预览页'),
          }),
        ),
      ),
    );
  });

  await flushAsyncWork();
  return container;
}

describe('MerchantProductEdit', () => {
  it('renders field impact guidance for merchant edits', async () => {
    const element = await renderMerchantProductEdit();

    expect(element.textContent).toContain('字段影响提示');
    expect(element.textContent).toContain('用户侧展示');
    expect(element.textContent).toContain('结算金额');
    expect(element.textContent).toContain('库存校验');
    expect(element.textContent).toContain('履约方式');
  });

  it('opens the user-facing product preview from the edit header', async () => {
    const element = await renderMerchantProductEdit();
    const previewButton = element.querySelector<HTMLButtonElement>('button[aria-label="预览用户视图"]');

    expect(previewButton).toBeTruthy();

    await act(async () => {
      previewButton?.dispatchEvent(new MouseEvent('click', { bubbles: true }));
    });
    await flushAsyncWork();

    expect(element.textContent).toContain('用户商品预览页');
  });
});

function buildProduct(): MerchantProduct {
  return {
    id: 42,
    tenantId: 9,
    productCode: 'SKU-42',
    name: '本地测试商品',
    price: 19900,
    unit: '件',
    category: '测试类目',
    description: '测试商品描述',
    imageUrl: null,
    stock: 8,
    status: 'active',
    productType: 'PHYSICAL',
    fulfillmentMode: 'EXPRESS_DELIVERY',
    storeId: null,
    virtualTypeId: null,
    virtualCategoryId: null,
    deliveryConfig: '',
    createTime: '2026-07-08T10:00:00',
    updateTime: null,
  };
}
