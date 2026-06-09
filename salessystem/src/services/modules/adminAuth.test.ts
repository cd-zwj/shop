import { describe, it, expect, vi, beforeEach } from 'vitest';

const mockRequest = vi.fn();
vi.mock('../request', () => ({
  request: (...args: unknown[]) => mockRequest(...args),
}));

import { adminAuthService } from './adminAuth';

beforeEach(() => {
  vi.clearAllMocks();
});

describe('adminAuthService', () => {
  describe('login', () => {
    it('应调用 POST /v1/admin/auth/login 并携带登录数据', async () => {
      // Arrange
      const payload = {
        username: 'admin',
        password: 'admin123',
        captchaKey: 'key',
        captchaCode: 'code',
      };
      mockRequest.mockResolvedValue('admin-token-xyz');

      // Act
      const result = await adminAuthService.login(payload);

      // Assert
      expect(mockRequest).toHaveBeenCalledWith({
        url: '/v1/admin/auth/login',
        method: 'post',
        data: payload,
        authRole: false,
      });
      expect(result).toBe('admin-token-xyz');
    });
  });

  describe('getCurrentSession', () => {
    it('应调用 GET /v1/admin/auth/session 且 authRole 为 admin', async () => {
      // Arrange
      const session = {
        userId: 1,
        username: 'admin',
        role: 'SUPER_ADMIN',
        scope: 'all',
        permissions: ['*'],
        roles: ['admin'],
      };
      mockRequest.mockResolvedValue(session);

      // Act
      const result = await adminAuthService.getCurrentSession();

      // Assert
      expect(mockRequest).toHaveBeenCalledWith({
        url: '/v1/admin/auth/session',
        method: 'get',
        authRole: 'admin',
      });
      expect(result).toEqual(session);
    });
  });

  describe('logout', () => {
    it('应调用 POST /v1/admin/auth/logout 且 authRole 为 admin', async () => {
      // Arrange
      mockRequest.mockResolvedValue(undefined);

      // Act
      await adminAuthService.logout();

      // Assert
      expect(mockRequest).toHaveBeenCalledWith({
        url: '/v1/admin/auth/logout',
        method: 'post',
        authRole: 'admin',
      });
    });
  });

  describe('错误处理', () => {
    it('管理员登录失败时应将错误向上抛出', async () => {
      // Arrange
      const error = new Error('验证码错误');
      mockRequest.mockRejectedValue(error);

      // Act & Assert
      await expect(
        adminAuthService.login({
          username: 'admin',
          password: 'pass',
          captchaKey: 'key',
          captchaCode: 'wrong',
        }),
      ).rejects.toThrow('验证码错误');
    });

    it('获取 session 失败时应将错误向上抛出', async () => {
      // Arrange
      mockRequest.mockRejectedValue(new Error('未授权'));

      // Act & Assert
      await expect(adminAuthService.getCurrentSession()).rejects.toThrow('未授权');
    });
  });
});
