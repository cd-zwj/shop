import { expect, test, type Page, type Route } from '@playwright/test';

type ApiEnvelope<T> = {
  code: number;
  message: string;
  data: T;
};

function ok<T>(data: T): ApiEnvelope<T> {
  return { code: 200, message: 'success', data };
}

async function json(route: Route, data: unknown) {
  await route.fulfill({
    status: 200,
    contentType: 'application/json',
    body: JSON.stringify(data),
  });
}

async function seedUserSession(page: Page) {
  await page.addInitScript(() => {
    localStorage.setItem('salessystem:app:token', 'store-pickup-e2e-token');
    localStorage.setItem('salessystem:current-role', 'user');
    localStorage.setItem('salessystem:user:profile', JSON.stringify({
      id: 99,
      username: 'pickup_e2e_user',
      nickname: '自提测试用户',
    }));
  });
}

async function mockPickupApis(page: Page, submittedPayloads: unknown[]) {
  await page.route('**/api/v1/app/users/me', (route) => json(route, ok({
    id: 99,
    username: 'pickup_e2e_user',
    nickname: '自提测试用户',
  })));
  await page.route('**/api/v1/app/notifications/unread-count', (route) => json(route, ok({ count: 0 })));
  await page.route('**/api/v1/app/tenants/9', (route) => json(route, ok({
    id: 9,
    name: '晨光门店',
    address: '测试市测试区自提路 1 号',
    status: 1,
  })));
  await page.route('**/api/v1/app/tenants/9/coupons/available', (route) => json(route, ok([])));
  await page.route('**/api/v1/app/tenants/9/coupons?**', (route) => json(route, ok([])));
  await page.route('**/api/v1/app/products/*?**', (route) => {
    const storeId = Number(new URL(route.request().url()).searchParams.get('storeId'));
    return json(route, ok({
      id: storeId === 11 ? 101 : 102,
      tenantId: 9,
      storeId,
      name: storeId === 11 ? '门店一号商品' : '门店二号商品',
      price: storeId === 11 ? 29.9 : 39.9,
      stock: 8,
      category: '日用百货',
      fulfillmentMode: 'STORE_PICKUP',
      status: 1,
      purchasable: true,
      fulfillmentLabel: '到店自提',
      fulfillmentDescription: '下单后到门店自提',
    }));
  });
  await page.route('**/api/v1/app/orders', async (route) => {
    if (route.request().method() !== 'POST') {
      await json(route, ok({ records: [], total: 0, page: 1, size: 10, pages: 0 }));
      return;
    }
    submittedPayloads.push(route.request().postDataJSON());
    await json(route, ok({
      orderNo: 'SO-PICKUP-001',
      orderStatus: 'WAIT_PAY',
      payStatus: 'WAIT_PAY',
      totalAmount: 29.9,
      payableAmount: 29.9,
    }));
  });
  await page.route('**/api/v1/app/orders/SO-PICKUP-001', (route) => json(route, ok({
    orderNo: 'SO-PICKUP-001',
    orderStatus: 'WAIT_PAY',
    payStatus: 'WAIT_PAY',
    fulfillmentMode: 'STORE_PICKUP',
    storeId: 11,
    orderItems: [],
  })));
}

async function mockDeliveredPickupOrder(page: Page) {
  await page.route('**/api/v1/app/refunds?**', (route) => json(route, ok({
    records: [], total: 0, page: 1, current: 1, size: 100, pages: 0,
  })));
  await page.route('**/api/v1/app/orders/SO-PICKUP-DELIVERED', (route) => json(route, ok({
    id: 201,
    orderNo: 'SO-PICKUP-DELIVERED',
    tenantId: 9,
    platformUserId: 99,
    orderStatus: 'PAID',
    payStatus: 'SUCCESS',
    totalAmount: 2990,
    payableAmount: 2990,
    externalPayAmount: 2990,
    subject: '门店自提订单',
    storeId: 11,
    fulfillmentMode: 'STORE_PICKUP',
    paymentBillStatus: 'SUCCESS',
    items: [{
      id: 501,
      orderId: 201,
      orderNo: 'SO-PICKUP-DELIVERED',
      tenantId: 9,
      productId: 101,
      productName: '门店一号商品',
      price: 2990,
      quantity: 1,
      subtotal: 2990,
      deliveryStatus: 'DELIVERED',
      pickupCode: '12345678',
    }],
  })));
}

test('购物车仅保留一个自提门店，并以自提载荷创建订单', async ({ page }) => {
  const submittedPayloads: unknown[] = [];
  await seedUserSession(page);
  await mockPickupApis(page, submittedPayloads);

  await page.goto('/product/101?tenantId=9&storeId=11');
  await expect(page.getByText('门店一号商品').first()).toBeVisible();
  await page.getByRole('button', { name: '加入购物车' }).first().click();
  await expect(page.locator('body')).toContainText('已加入购物车');

  await page.goto('/product/102?tenantId=9&storeId=12');
  await expect(page.getByText('门店二号商品').first()).toBeVisible();
  await page.getByRole('button', { name: '加入购物车' }).first().click();
  await expect(page.locator('body')).toContainText('购物车已包含其他门店商品');

  await page.goto('/cart');
  await expect(page.locator('body')).toContainText('门店一号商品');
  await expect(page.locator('body')).not.toContainText('门店二号商品');
  await page.getByRole('button', { name: '结算该商户商品' }).click();

  await expect.poll(() => submittedPayloads).toHaveLength(1);
  expect(submittedPayloads[0]).toMatchObject({
    tenantId: 9,
    storeId: 11,
    fulfillmentMode: 'STORE_PICKUP',
    items: [{ productId: 101, quantity: 1, price: 29.9 }],
  });
  expect(submittedPayloads[0]).not.toHaveProperty('addressId');
});

test('已交付订单按订单项展示取货码', async ({ page }) => {
  await seedUserSession(page);
  await mockPickupApis(page, []);
  await mockDeliveredPickupOrder(page);

  await page.goto('/order/SO-PICKUP-DELIVERED');

  await expect(page.getByText('门店一号商品')).toBeVisible();
  await expect(page.getByText('取货码', { exact: true })).toBeVisible();
  await expect(page.getByText('12345678', { exact: true })).toBeVisible();
});
