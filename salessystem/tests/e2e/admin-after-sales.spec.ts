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

async function seedAdminSession(page: Page) {
  await page.addInitScript(() => {
    localStorage.setItem('salessystem:platform:token', 'admin-after-sale-e2e-token');
    localStorage.setItem('salessystem:current-role', 'admin');
    localStorage.setItem('salessystem:admin:session', JSON.stringify({
      userId: 1,
      username: 'admin_after_sale',
      role: 'ADMIN',
      scope: 'PLATFORM',
      roles: ['ADMIN'],
      permissions: ['admin:after-sale:list', 'admin:after-sale:manage'],
    }));
  });
}

test('平台管理员查看跨租户售后流水并提交带版本状态的介入决定', async ({ page }) => {
  const submittedPayloads: unknown[] = [];
  const refund = {
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
    reason: '包装破损',
    description: '到店检查时发现外包装破损',
    evidenceUrls: ['https://example.test/evidence.jpg'],
    rejectReason: null,
    auditTime: null,
    completeTime: null,
    createTime: '2026-08-05 10:00:00',
    updateTime: '2026-08-05 10:00:00',
  };

  await seedAdminSession(page);
  await page.route('**/api/v1/admin/auth/session', (route) => json(route, ok({
    userId: 1,
    username: 'admin_after_sale',
    role: 'ADMIN',
    scope: 'PLATFORM',
    roles: ['ADMIN'],
    permissions: ['admin:after-sale:list', 'admin:after-sale:manage'],
  })));
  await page.route('**/api/v1/admin/refunds?**', (route) => json(route, ok({
    records: [refund], total: 1, page: 1, current: 1, size: 20, pages: 1,
  })));
  await page.route('**/api/v1/admin/tenants/9/refunds/1/actions', (route) => json(route, ok([
    {
      action: 'USER_APPLY',
      operatorRole: 'USER',
      remark: '包装破损',
      evidenceUrls: [],
      createTime: '2026-08-05 10:00:00',
    },
  ])));
  await page.route('**/api/v1/admin/tenants/9/refunds/1', (route) => json(route, ok(refund)));
  await page.route('**/api/v1/admin/tenants/9/refunds/1/intervene', async (route) => {
    submittedPayloads.push(route.request().postDataJSON());
    await json(route, ok(null));
  });

  await page.goto('/admin/after-sales');
  await expect(page.getByText('RA-ADMIN-1')).toBeVisible();
  await expect(page.getByText('租户 9')).toBeVisible();
  await expect(page.getByText('¥12.80')).toBeVisible();

  await page.getByRole('button', { name: '查看详情' }).click();
  await expect(page.getByText('提交申请')).toBeVisible();
  await expect(page.getByRole('link', { name: /凭证 1/ })).toHaveAttribute(
    'href',
    'https://example.test/evidence.jpg',
  );

  await page.getByRole('textbox', { name: '平台处理说明' }).fill('  核对订单与凭证后同意退款  ');
  await page.getByRole('button', { name: '确认同意退款' }).click();

  await expect.poll(() => submittedPayloads).toEqual([{
    approved: true,
    expectedStatus: 'PENDING',
    remark: '核对订单与凭证后同意退款',
  }]);
});
