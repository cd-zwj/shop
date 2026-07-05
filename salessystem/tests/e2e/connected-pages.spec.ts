import { expect, test } from '@playwright/test';
import { execFileSync } from 'node:child_process';

const defaultUser = process.env.E2E_USER_NAME || 'user';
const defaultPassword = process.env.E2E_USER_PASSWORD || 'admin123';
const redisContainer = process.env.E2E_REDIS_CONTAINER || 'payment-system-redis-1';

type ApiEnvelope<T> = {
  code: number;
  message: string;
  data: T;
  timestamp?: number;
};

type CaptchaPayload = {
  captchaKey: string;
  captchaImage: string;
};

function readCaptchaCode(captchaKey: string) {
  const redisKey = 'auth:captcha:' + captchaKey;
  const value = execFileSync('docker', ['exec', redisContainer, 'redis-cli', 'GET', redisKey], {
    encoding: 'utf8',
    stdio: ['ignore', 'pipe', 'pipe'],
  }).trim();
  expect(value, 'Redis captcha code should exist for ' + redisKey).toBeTruthy();
  return value;
}

test.describe('前后端联通页面流', () => {
  test('登录页加载并从后端获取图形验证码', async ({ page }) => {
    const captchaResponse = page.waitForResponse((response) =>
      response.url().includes('/v1/auth/captcha') && response.status() === 200,
    );
    await page.goto('/login');
    await captchaResponse;
    await expect(page.getByRole('heading', { name: '登录你的账户' })).toBeVisible();
    await expect(page.getByRole('img', { name: '登录验证码' })).toBeVisible();
    await expect(page.getByRole('button', { name: /确认登录/ })).toBeVisible();
  });

  test('未登录访问首页会跳转到登录页', async ({ page }) => {
    await page.goto('/');
    await expect(page).toHaveURL(/\/login$/);
    await expect(page.getByRole('heading', { name: '登录你的账户' })).toBeVisible();
  });

  test('默认用户可通过真实后端登录并进入首页', async ({ page }) => {
    // Collect all captcha responses (React StrictMode fires effect twice)
    const captchaResponses: any[] = [];
    page.on('response', (response) => {
      if (response.url().includes('/v1/auth/captcha') && response.status() === 200) {
        captchaResponses.push(response);
      }
    });

    await page.goto('/login');

    // Wait for captcha image to appear (means at least one captcha loaded)
    await expect(page.getByRole('img', { name: '登录验证码' })).toBeVisible();

    // Wait a bit for the second StrictMode captcha to arrive
    await page.waitForTimeout(1000);

    // Use the LAST captcha response (the one whose key is actually in React state)
    const lastCaptchaResponse = captchaResponses[captchaResponses.length - 1];
    expect(lastCaptchaResponse, 'At least one captcha response should have been captured').toBeTruthy();
    const captchaBody = (await lastCaptchaResponse.json()) as ApiEnvelope<CaptchaPayload>;
    expect(captchaBody.code).toBe(200);

    const captchaCode = readCaptchaCode(captchaBody.data.captchaKey);

    await page.getByPlaceholder('用户名 / Email').fill(defaultUser);
    await page.getByPlaceholder('请输入密码').fill(defaultPassword);
    await page.getByPlaceholder('请输入图片中的字符').fill(captchaCode);

    const loginResponse = page.waitForResponse((response) =>
      response.url().includes('/v1/app/auth/login/password') && response.status() === 200,
    );
    await page.getByRole('button', { name: /确认登录/ }).click();
    const response = await loginResponse;
    const body = (await response.json()) as ApiEnvelope<string>;
    expect(body.code).toBe(200);

    await expect(page).toHaveURL(/\/$/);
    await expect(page.getByPlaceholder('搜索商户、商品或分类...')).toBeVisible();
  });
});
