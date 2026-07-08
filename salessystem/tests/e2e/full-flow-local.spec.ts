import { expect, test, type Page } from '@playwright/test';
import { execFileSync } from 'node:child_process';

const apiBaseURL = process.env.E2E_API_BASE_URL || 'http://127.0.0.1:8080/api';
const redisContainer = process.env.E2E_REDIS_CONTAINER || 'payment-system-redis-1';

const userName = process.env.E2E_USER_NAME || 'e2e_user_20260626151655';
const userPassword = process.env.E2E_USER_PASSWORD || 'E2ePass123';
const adminName = process.env.E2E_ADMIN_NAME || 'admin';
const merchantName = process.env.E2E_MERCHANT_NAME || 'merchant';
const defaultPassword = process.env.E2E_DEFAULT_PASSWORD || 'admin123';

type ApiEnvelope<T> = {
  code: number;
  message: string;
  data: T;
};

type CaptchaPayload = {
  captchaKey: string;
  captchaImage: string;
};

type AddressPayload = {
  id: number;
  isDefault?: number;
};

async function requestJson<T>(path: string, init?: RequestInit) {
  const response = await fetch(`${apiBaseURL}${path}`, {
    ...init,
    headers: { 'Content-Type': 'application/json', ...(init?.headers ?? {}) },
  });
  const body = (await response.json()) as ApiEnvelope<T>;
  expect(body.code, `${path}: ${body.message}`).toBe(200);
  return body.data;
}

async function getCaptcha() {
  const captcha = await requestJson<CaptchaPayload>('/v1/auth/captcha');
  const code = execFileSync('docker', ['exec', redisContainer, 'redis-cli', 'GET', `auth:captcha:${captcha.captchaKey}`], {
    encoding: 'utf8',
    stdio: ['ignore', 'pipe', 'pipe'],
  }).trim();
  expect(code).toBeTruthy();
  return { ...captcha, code };
}

async function reloadPageCaptcha(page: Page) {
  for (let attempt = 0; attempt < 3; attempt += 1) {
    const captchaResponse = page.waitForResponse((response) =>
      response.url().includes('/v1/auth/captcha') && response.status() === 200,
    );
    await page.getByRole('img', { name: '登录验证码' }).click();
    const response = await captchaResponse;
    const body = (await response.json()) as ApiEnvelope<CaptchaPayload | null>;
    if (body.code === 200 && body.data?.captchaKey) {
      const code = execFileSync('docker', ['exec', redisContainer, 'redis-cli', 'GET', `auth:captcha:${body.data.captchaKey}`], {
        encoding: 'utf8',
      }).trim();
      if (code) {
        return { ...body.data, code };
      }
    }
  }

  throw new Error('Unable to reload a valid page captcha');
}

async function loginToken(path: string, username: string, password: string) {
  const captcha = await getCaptcha();
  return requestJson<string>(path, {
    method: 'POST',
    body: JSON.stringify({
      username,
      password,
      captchaKey: captcha.captchaKey,
      captchaCode: captcha.code,
    }),
  });
}

async function loginMerchantSession() {
  const captcha = await getCaptcha();
  return requestJson<{ token: string; tenantId: number; username: string }>('/v1/merchant/auth/login', {
    method: 'POST',
    body: JSON.stringify({
      username: merchantName,
      password: defaultPassword,
      captchaKey: captcha.captchaKey,
      captchaCode: captcha.code,
    }),
  });
}

async function ensureDefaultAddress(authHeaders: Record<string, string>) {
  const addresses = await requestJson<AddressPayload[]>('/v1/app/addresses', {
    headers: authHeaders,
  });
  const existing = addresses.find((address) => address.isDefault === 1) ?? addresses[0];
  if (existing) {
    return existing;
  }

  return requestJson<AddressPayload>('/v1/app/addresses', {
    method: 'POST',
    headers: authHeaders,
    body: JSON.stringify({
      receiverName: 'E2E测试用户',
      phone: '13800138000',
      province: '浙江省',
      city: '杭州市',
      district: '西湖区',
      detail: '本地联调测试地址1号',
      isDefault: true,
    }),
  });
}

async function seedBrowserSession(page: Page, role: 'user' | 'admin' | 'merchant') {
  if (role === 'user') {
    const token = await loginToken('/v1/app/auth/login/password', userName, userPassword);
    const profile = await requestJson<Record<string, unknown>>('/v1/app/users/me', {
      headers: { Authorization: `Bearer ${token}`, satoken: `Bearer ${token}` },
    });
    await page.addInitScript(
      ({ token, profile }) => {
        localStorage.setItem('salessystem:app:token', token);
        localStorage.setItem('salessystem:current-role', 'user');
        localStorage.setItem('salessystem:user:profile', JSON.stringify(profile));
      },
      { token, profile },
    );
    return;
  }

  if (role === 'admin') {
    const token = await loginToken('/v1/admin/auth/login', adminName, defaultPassword);
    const session = await requestJson<Record<string, unknown>>('/v1/admin/auth/session', {
      headers: { Authorization: `Bearer ${token}`, satoken: `Bearer ${token}` },
    });
    await page.addInitScript(
      ({ token, session }) => {
        localStorage.setItem('salessystem:platform:token', token);
        localStorage.setItem('salessystem:current-role', 'admin');
        localStorage.setItem('salessystem:admin:session', JSON.stringify(session));
      },
      { token, session },
    );
    return;
  }

  const session = await loginMerchantSession();
  await page.addInitScript(
    ({ session }) => {
      localStorage.setItem('salessystem:merchant:token', session.token);
      localStorage.setItem('salessystem:current-role', 'merchant');
      localStorage.setItem('salessystem:merchant:session', JSON.stringify(session));
    },
    { session },
  );
}

async function expectPageReady(page: Page, path: string, marker: RegExp | string) {
  await page.goto(path, { waitUntil: 'domcontentloaded' });
  await expect(page.locator('body')).toContainText(marker, { timeout: 15_000 });
}

test.describe('本地全流程冒烟测试', () => {
  test('注册测试用户可通过登录页登录', async ({ page }) => {
    await page.goto('/login');
    await expect(page.getByRole('heading', { name: '登录你的账户' })).toBeVisible();

    const captcha = await getCaptcha();
    await page.getByPlaceholder('用户名 / Email').fill(userName);
    await page.getByPlaceholder('请输入密码').fill(userPassword);
    await page.getByPlaceholder('请输入图片中的字符').fill(captcha.code);
    await page.evaluate(({ key }) => {
      const input = document.querySelector('input[placeholder="请输入图片中的字符"]') as HTMLInputElement | null;
      if (input) input.dispatchEvent(new Event('input', { bubbles: true }));
      window.localStorage.setItem('__e2e_captcha_key_seen', key);
    }, { key: captcha.captchaKey });

    // The UI owns captchaKey state, so reload the captcha through the page and use the current key.
    const pageCaptcha = await reloadPageCaptcha(page);
    await page.getByPlaceholder('请输入图片中的字符').fill(pageCaptcha.code);

    await page.getByRole('button', { name: /确认登录/ }).click();
    await expect(page).toHaveURL(/\/$/);
    await expect(page.locator('body')).toContainText(/SalesSystem|搜索商户|首页/, { timeout: 15_000 });
  });

  test('C 端主要功能页面可访问并返回真实后端数据', async ({ page }) => {
    await seedBrowserSession(page, 'user');

    const token = await loginToken('/v1/app/auth/login/password', userName, userPassword);
    const authHeaders = { Authorization: `Bearer ${token}`, satoken: `Bearer ${token}` };
    const tenants = await requestJson<Array<{ id: number; name: string }>>('/v1/app/tenants', {
      headers: authHeaders,
    });
    expect(tenants.length).toBeGreaterThan(0);
    const tenantId = tenants[0].id;
    const products = await requestJson<Array<{ id: number; price: number }>>(`/v1/app/tenants/${tenantId}/products`, {
      headers: authHeaders,
    });
    expect(products.length).toBeGreaterThan(0);
    const address = await ensureDefaultAddress(authHeaders);

    await expectPageReady(page, '/', /SalesSystem|搜索商户|首页/);
    await expectPageReady(page, `/merchant-store/${tenantId}`, /商品|商户|E2E/);
    await expectPageReady(page, `/product/${products[0].id}`, /E2E|加入|购买|商品/);
    await expectPageReady(page, '/cart', /购物车|结算|空/);
    await expectPageReady(page, '/wallet', /钱包|余额/);
    await expectPageReady(page, '/orders', /订单|暂无/);
    await expectPageReady(page, '/coupons', /优惠券|E2E|暂无/);
    await expectPageReady(page, `/points/${tenantId}`, /积分|500|暂无/);
    await expectPageReady(page, `/growth/${tenantId}`, /成长|会员|暂无/);
    await expectPageReady(page, '/addresses', /地址|新增|暂无/);
    await expectPageReady(page, '/notifications', /通知|消息|暂无/);
    await expectPageReady(page, '/account-security', /安全|密码|账号/);

    const order = await requestJson<{ orderNo: string; payStatus: string }>('/v1/app/orders', {
      method: 'POST',
      headers: authHeaders,
      body: JSON.stringify({
        tenantId,
        totalAmount: products[0].price,
        subject: 'E2E smoke order',
        source: 'E2E',
        walletStrategy: 'UNIFIED_ONLY',
        allowExternalPayFallback: false,
        addressId: address.id,
        items: [{ productId: products[0].id, quantity: 1, price: products[0].price }],
      }),
    });
    expect(order.orderNo).toBeTruthy();
  });

  test('商户端主要功能页面可访问', async ({ page }) => {
    await seedBrowserSession(page, 'merchant');
    await expectPageReady(page, '/merchant', /商户|工作台|订单/);
    await expectPageReady(page, '/merchant/products', /商品|新增|E2E/);
    await expectPageReady(page, '/merchant/orders', /订单|暂无/);
    await expectPageReady(page, '/merchant/finance', /财务|账务核对|余额|提现/);
    await expectPageReady(page, '/merchant/marketing/coupons', /优惠券|营销|暂无/);
    await expectPageReady(page, '/merchant/marketing/activities', /促销|活动|暂无/);
    await expectPageReady(page, '/merchant/marketing/members', /会员|等级|标签/);
    await expectPageReady(page, '/merchant/refunds', /退款|售后|暂无/);
    await expectPageReady(page, '/merchant/tasks?type=compensation', /系统任务|任务列表|暂无/);
    await expectPageReady(page, '/merchant/tasks?type=retry', /系统任务|任务列表|暂无/);
    await expectPageReady(page, '/merchant/rules', /规则|配置|充值/);
    await expectPageReady(page, '/merchant/withdrawals', /提现|财务|暂无/);
  });

  test('平台管理端主要功能页面可访问', async ({ page }) => {
    await seedBrowserSession(page, 'admin');
    await expectPageReady(page, '/admin', /管理|仪表盘|商户/);
    await expectPageReady(page, '/admin/merchants', /商户|入驻|默认/);
    await expectPageReady(page, '/admin/products', /商品|E2E|全量/);
    await expectPageReady(page, '/admin/analytics', /分析|数据|趋势/);
    await expectPageReady(page, '/admin/transactions', /流水|交易|暂无/);
    await expectPageReady(page, '/admin/payments', /支付|账单|暂无/);
    await expectPageReady(page, '/admin/recharges', /充值|监管|暂无/);
    await expectPageReady(page, '/admin/users', /用户|安全|治理/);
    await expectPageReady(page, '/admin/withdrawals', /提现|审批|暂无/);
    await expectPageReady(page, '/admin/marketing', /营销|优惠券|活动/);
    await expectPageReady(page, '/admin/documents', /知识库|文档|上传/);
  });
});
