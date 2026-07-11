import { expect, test, type Page, type Route } from '@playwright/test';

type ApiEnvelope<T> = {
  code: number;
  message: string;
  data: T;
};

type LifecycleStep = 'available' | 'received' | 'locked' | 'used';

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
    localStorage.setItem('salessystem:app:token', 'e2e-token');
    localStorage.setItem('salessystem:current-role', 'user');
    localStorage.setItem('salessystem:user:profile', JSON.stringify({
      id: 99,
      username: 'asset_e2e_user',
      nickname: '资产联动用户',
    }));
  });
}

async function mockAssetLifecycleApis(page: Page) {
  let step: LifecycleStep = 'available';

  await page.route('**/api/v1/app/users/me', (route) => json(route, ok({
    id: 99,
    username: 'asset_e2e_user',
    nickname: '资产联动用户',
  })));

  await page.route('**/api/v1/app/tenants', (route) => json(route, ok([
    { id: 9, name: '本地测试店', status: 1 },
  ])));

  await page.route('**/api/v1/app/tenants/9/coupons/available', (route) => json(route, ok(step === 'available' ? [{
    id: 201,
    name: '满 30 减 8',
    ownerType: 'TENANT',
    couponType: 'FIXED',
    thresholdAmount: 30,
    discountAmount: 8,
    discountRate: null,
    maxDiscountAmount: null,
    remainingStock: 20,
    perUserLimit: 1,
    receivedByCurrentUser: 0,
    receivable: true,
    receiveStartTime: '2026-07-01T00:00:00',
    receiveEndTime: '2026-07-31T23:59:59',
    validStartTime: '2026-07-01T00:00:00',
    validEndTime: '2026-07-31T23:59:59',
  }] : [])));

  const listUserCoupons = (route: Route) => {
    const url = new URL(route.request().url());
    const status = url.searchParams.get('status');
    const hasCoupon = step !== 'available';
    const isUsable = status === 'RECEIVED';
    const isUsed = status === 'USED';
    const shouldReturnCoupon = (!status && hasCoupon)
      || (isUsable && (step === 'received' || step === 'locked'))
      || (isUsed && step === 'used')
      || false;

    return json(route, ok(shouldReturnCoupon ? [{
      id: 501,
      couponNo: 'UC501',
      templateId: 201,
      tenantId: 9,
      couponStatus: step === 'used' ? 'USED' : step === 'locked' ? 'LOCKED' : 'RECEIVED',
      orderNo: step === 'available' || step === 'received' ? null : 'SO-ASSET-001',
      templateName: '满 30 减 8',
      couponType: 'FIXED',
      thresholdAmount: 30,
      discountAmount: 8,
      receiveTime: '2026-07-10T09:00:00',
      expireTime: '2026-07-31T23:59:59',
      useTime: step === 'used' ? '2026-07-10T09:08:00' : null,
      timeline: [
        { eventType: 'RECEIVE', title: '优惠券领取', occurredAt: '2026-07-10T09:00:00', description: '优惠券已进入账户', bizNo: 'CR-ASSET-001' },
        ...(step === 'locked' || step === 'used' ? [
          { eventType: 'LOCK', title: '优惠券锁定', occurredAt: '2026-07-10T09:05:00', description: '订单锁定优惠券', orderNo: 'SO-ASSET-001' },
        ] : []),
        ...(step === 'used' ? [
          { eventType: 'WRITE_OFF', title: '优惠券核销', occurredAt: '2026-07-10T09:08:00', description: '支付成功后核销优惠券', orderNo: 'SO-ASSET-001' },
        ] : []),
      ],
    }] : []));
  };

  await page.route('**/api/v1/app/tenants/9/coupons', listUserCoupons);
  await page.route('**/api/v1/app/tenants/9/coupons?**', listUserCoupons);

  await page.route('**/api/v1/app/tenants/9/coupons/201/receive', (route) => {
    step = 'received';
    return json(route, ok({ userCouponId: 501, couponNo: 'UC501' }));
  });

  await page.route('**/api/v1/app/orders', (route) => {
    if (route.request().method() === 'POST') {
      step = 'locked';
      return json(route, ok({
        orderNo: 'SO-ASSET-001',
        orderStatus: 'WAIT_PAY',
        payStatus: 'WAIT_PAY',
        totalAmount: 36,
        discountAmount: 8,
        payableAmount: 28,
      }));
    }
    return json(route, ok({ records: [], total: 0, page: 1, size: 10, pages: 0 }));
  });

  await page.route('**/api/v1/open/payments/callbacks/e2e-asset', (route) => {
    step = 'used';
    return json(route, ok({ status: 'SUCCESS' }));
  });

  await page.route('**/api/v1/app/wallets/unified', (route) => json(route, ok({
    walletType: 'UNIFIED',
    tenantId: null,
    availableAmount: 128,
    frozenAmount: 0,
    totalRecharge: 200,
    totalConsume: 72,
  })));

  await page.route('**/api/v1/app/wallets/unified/logs?**', (route) => json(route, ok({
    records: [],
    total: 0,
    page: 1,
    size: 5,
    pages: 0,
  })));

  await page.route('**/api/v1/app/assets/tenant-summaries', (route) => json(route, ok([{
    tenantId: 9,
    tenantName: '本地测试店',
    memberStatus: 1,
    walletAvailableAmount: 0,
    walletFrozenAmount: 0,
    points: 120,
    expiringSoonPoints: 0,
    usableCouponCount: step === 'used' ? 0 : 1,
    lockedCouponCount: step === 'locked' ? 1 : 0,
    usedCouponCount: step === 'used' ? 1 : 0,
    expiredCouponCount: 0,
    expiringSoonCouponCount: step === 'used' ? 0 : 1,
    totalGrowth: 260,
  }])));

  await page.route('**/api/v1/app/assets/activities?**', (route) => json(route, ok([
    ...(step === 'used' ? [{
      assetType: 'COUPON',
      title: '优惠券核销',
      description: '订单 SO-ASSET-001 已使用优惠券',
      occurredAt: '2026-07-10T09:08:00',
      tenantId: 9,
      tenantName: '本地测试店',
      bizNo: 'SO-ASSET-001',
      amountText: '¥8',
      tone: 'positive',
      actionPath: '/coupons?tenantId=9',
    }] : []),
    ...(step === 'locked' || step === 'used' ? [{
      assetType: 'COUPON',
      title: '优惠券锁定',
      description: '订单 SO-ASSET-001 锁定优惠券',
      occurredAt: '2026-07-10T09:05:00',
      tenantId: 9,
      tenantName: '本地测试店',
      bizNo: 'SO-ASSET-001',
      amountText: null,
      tone: 'neutral',
      actionPath: '/coupons?tenantId=9',
    }] : []),
    {
      assetType: 'COUPON',
      title: '优惠券领取',
      description: '优惠券已进入账户',
      occurredAt: '2026-07-10T09:00:00',
      tenantId: 9,
      tenantName: '本地测试店',
      bizNo: 'CR-ASSET-001',
      amountText: null,
      tone: 'positive',
      actionPath: '/coupons?tenantId=9',
    },
  ])));
}

test('优惠券领取、锁定、核销后资产中心展示统一动态与提醒', async ({ page }) => {
  await seedUserSession(page);
  await mockAssetLifecycleApis(page);

  await page.goto('/coupons?tenantId=9');
  await expect(page.getByRole('heading', { name: '优惠券中心' })).toBeVisible();
  await page.getByRole('button', { name: '立即领取' }).click();
  await expect(page.locator('body')).toContainText('满 30 减 8');

  await page.evaluate(async () => {
    await fetch('http://localhost:8080/api/v1/app/orders', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ selectedUserCouponId: 501 }),
    });
    await fetch('http://localhost:8080/api/v1/open/payments/callbacks/e2e-asset', { method: 'POST' });
  });

  await page.goto('/wallet');
  await expect(page.getByRole('heading', { name: '我的钱包' })).toBeVisible();
  await expect(page.locator('body')).toContainText('统一资产动态');
  await expect(page.locator('body')).toContainText('优惠券领取');
  await expect(page.locator('body')).toContainText('优惠券锁定');
  await expect(page.locator('body')).toContainText('优惠券核销');
  await expect(page.locator('body')).toContainText('成长值 260');
});
