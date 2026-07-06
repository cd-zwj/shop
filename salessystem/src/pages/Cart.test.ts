import React, { act } from 'react';
import { createRoot, type Root } from 'react-dom/client';
import { MemoryRouter } from 'react-router-dom';
import { afterEach, describe, expect, it, vi } from 'vitest';
import Cart from './Cart';
import { appCatalogService } from '../services/modules/appCatalog';
import { appCouponService } from '../services/modules/appCoupon';
import { appOrderService } from '../services/modules/appOrder';

vi.mock('motion/react', () => ({
  motion: new Proxy({}, {
    get: (_target, tag: string) => tag,
  }),
}));

const mockReplaceTenantItems = vi.fn();
const mockClearTenantItems = vi.fn();
const mockRemoveItem = vi.fn();
const mockUpdateQuantity = vi.fn();
const mockShowToast = vi.fn();
const cartItems = [{
  productId: 10,
  tenantId: 3,
  name: '测试商品',
  price: 100,
  quantity: 1,
  imageUrl: null,
  stock: 10,
  category: '虚拟商品',
  productType: 'CARD_KEY',
  fulfillmentMode: 'ONLINE_VIRTUAL',
}];

vi.mock('../context/AuthContext', () => ({
  useAuth: () => ({
    currentRole: 'user',
  }),
}));

vi.mock('../context/CartContext', () => ({
  useCart: () => ({
    items: cartItems,
    totalItems: 1,
    updateQuantity: mockUpdateQuantity,
    removeItem: mockRemoveItem,
    clearTenantItems: mockClearTenantItems,
    replaceTenantItems: mockReplaceTenantItems,
  }),
}));

vi.mock('../context/ToastContext', () => ({
  useToast: () => ({
    showToast: mockShowToast,
  }),
}));

vi.mock('../services/modules/appCatalog', () => ({
  appCatalogService: {
    getTenant: vi.fn(),
    getProduct: vi.fn(),
  },
}));

vi.mock('../services/modules/appCoupon', () => ({
  appCouponService: {
    getAvailableCoupons: vi.fn(),
    getMyCoupons: vi.fn(),
    claimCoupon: vi.fn(),
  },
}));

vi.mock('../services/modules/appOrder', () => ({
  appOrderService: {
    createOrder: vi.fn(),
  },
}));

const mockedCatalogService = vi.mocked(appCatalogService);
const mockedCouponService = vi.mocked(appCouponService);
const mockedOrderService = vi.mocked(appOrderService);

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

async function renderCart() {
  container = document.createElement('div');
  document.body.appendChild(container);
  root = createRoot(container);

  await act(async () => {
    root?.render(
      React.createElement(
        MemoryRouter,
        null,
        React.createElement(Cart),
      ),
    );
  });

  await flushAsyncWork();
  return container;
}

function buildTemplateCoupon(remainingStock: number) {
  return {
    id: 7,
    tenantId: 3,
    ownerType: 'TENANT' as const,
    name: '满百减十',
    couponType: 'FIXED' as const,
    thresholdAmount: 100,
    discountAmount: 10,
    discountRate: null,
    maxDiscountAmount: null,
    perUserLimit: 1,
    remainingStock,
    receivedByCurrentUser: 0,
    receivable: remainingStock > 0,
    receiveStartTime: '2026-07-01T00:00:00',
    receiveEndTime: '2026-07-31T23:59:59',
    validStartTime: null,
    validEndTime: null,
    validDaysAfterReceive: 7,
    description: null,
  };
}

describe('Cart', () => {
  it('revalidates selected coupon before checkout and blocks stale coupon usage', async () => {
    mockedCatalogService.getTenant.mockResolvedValue({ id: 3, name: '测试店铺' });
    mockedCatalogService.getProduct.mockResolvedValue({
      id: 10,
      tenantId: 3,
      name: '测试商品',
      price: 100,
      stock: 10,
      status: 1,
      category: '虚拟商品',
      imageUrl: null,
      productType: 'CARD_KEY',
      fulfillmentMode: 'ONLINE_VIRTUAL',
    });
    mockedCouponService.getAvailableCoupons
      .mockResolvedValueOnce([buildTemplateCoupon(5)])
      .mockResolvedValueOnce([buildTemplateCoupon(0)]);
    mockedCouponService.getMyCoupons.mockResolvedValue([]);
    mockedCouponService.claimCoupon.mockResolvedValue({
      userCouponId: 77,
      couponNo: 'UC77',
      couponTemplateId: 7,
      tenantId: 3,
      status: 'USABLE',
      expireTime: '2026-08-01T00:00:00',
    });
    mockedOrderService.createOrder.mockResolvedValue({
      orderNo: 'SO001',
      orderStatus: 'CREATED',
      payStatus: 'WAIT_PAY',
      totalAmount: 100,
      unifiedWalletDeductAmount: 0,
      merchantWalletDeductAmount: 0,
      externalPayAmount: 90,
      paymentBillNo: 'PB001',
      externalPayUrl: null,
      reusedPaymentBill: false,
    });

    const element = await renderCart();
    const couponSelect = Array.from(element.querySelectorAll('select'))
      .find((select) => select.textContent?.includes('满百减十'));
    expect(couponSelect).toBeTruthy();

    await act(async () => {
      couponSelect?.dispatchEvent(new Event('change', { bubbles: true }));
      if (couponSelect) {
        couponSelect.value = 'template-7';
        couponSelect.dispatchEvent(new Event('change', { bubbles: true }));
      }
    });
    await flushAsyncWork();

    const checkoutButton = Array.from(element.querySelectorAll('button'))
      .find((button) => button.textContent?.includes('结算该商户商品'));
    expect(checkoutButton).toBeTruthy();

    await act(async () => {
      checkoutButton?.dispatchEvent(new MouseEvent('click', { bubbles: true }));
    });
    await flushAsyncWork();

    expect(mockedCouponService.getAvailableCoupons).toHaveBeenCalledTimes(2);
    expect(mockedOrderService.createOrder).not.toHaveBeenCalled();
    expect(element.textContent).toContain('所选优惠券已取消');
    expect(element.textContent).toContain('无库存');
  });
});
