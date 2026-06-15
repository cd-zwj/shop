import { describe, it, expect, vi, beforeEach } from 'vitest';

// Mock request 模块 —— 所有 service 测试都拦截 ../request
const mockRequest = vi.fn();
vi.mock('../request', () => ({
  request: (...args: unknown[]) => mockRequest(...args),
}));

import { appAuthService } from './appAuth';

beforeEach(() => {
  vi.clearAllMocks();
});

describe('appAuthService', () => {
  describe('getCaptcha', () => {
    it('应调用 GET /v1/auth/captcha 且 authRole 为 false', async () => {
      // Arrange
      const captcha = { captchaKey: 'k1', captchaImage: 'base64...' };
      mockRequest.mockResolvedValue(captcha);

      // Act
      const result = await appAuthService.getCaptcha();

      // Assert
      expect(mockRequest).toHaveBeenCalledWith({
        url: '/v1/auth/captcha',
        method: 'get',
        authRole: false,
      });
      expect(result).toEqual(captcha);
    });
  });

  describe('register', () => {
    it('应调用 POST /v1/app/auth/register 并携带 payload', async () => {
      // Arrange
      const payload = { username: 'testuser', password: 'pass123' };
      const user = { id: 1, username: 'testuser' };
      mockRequest.mockResolvedValue(user);

      // Act
      const result = await appAuthService.register(payload);

      // Assert
      expect(mockRequest).toHaveBeenCalledWith({
        url: '/v1/app/auth/register',
        method: 'post',
        data: payload,
        authRole: false,
      });
      expect(result).toEqual(user);
    });
  });

  describe('loginByPassword', () => {
    it('应调用 POST /v1/app/auth/login/password', async () => {
      // Arrange
      const payload = {
        username: 'admin',
        password: '123456',
        captchaKey: 'key',
        captchaCode: 'code',
      };
      mockRequest.mockResolvedValue('token-abc');

      // Act
      const result = await appAuthService.loginByPassword(payload);

      // Assert
      expect(mockRequest).toHaveBeenCalledWith({
        url: '/v1/app/auth/login/password',
        method: 'post',
        data: payload,
        authRole: false,
      });
      expect(result).toBe('token-abc');
    });
  });


  describe('sendSmsCode', () => {
    it('应调用 POST /v1/app/auth/sms/send-code 并携带 phone + captcha', async () => {
      // Arrange
      const payload = {
        phone: '13800138000',
        captchaKey: 'key',
        captchaCode: 'code',
      };
      mockRequest.mockResolvedValue(undefined);

      // Act
      await appAuthService.sendSmsCode(payload);

      // Assert
      expect(mockRequest).toHaveBeenCalledWith({
        url: '/v1/app/auth/sms/send-code',
        method: 'post',
        data: payload,
        authRole: false,
      });
    });
  });

  describe('sendPasswordResetCode', () => {
    it('应调用 POST /v1/app/auth/password/reset/send-code 并携带 email + captcha', async () => {
      // Arrange
      const payload = {
        email: 'test@example.com',
        captchaKey: 'key',
        captchaCode: 'code',
      };
      mockRequest.mockResolvedValue(undefined);

      // Act
      await appAuthService.sendPasswordResetCode(payload);

      // Assert
      expect(mockRequest).toHaveBeenCalledWith({
        url: '/v1/app/auth/password/reset/send-code',
        method: 'post',
        data: payload,
        authRole: false,
      });
    });
  });

  describe('resetPassword', () => {
    it('应调用 POST /v1/app/auth/password/reset/verify 并携带 emailCode + newPassword', async () => {
      // Arrange
      const payload = {
        email: 'test@example.com',
        emailCode: '123456',
        newPassword: 'newPass123',
      };
      mockRequest.mockResolvedValue(undefined);

      // Act
      await appAuthService.resetPassword(payload);

      // Assert
      expect(mockRequest).toHaveBeenCalledWith({
        url: '/v1/app/auth/password/reset/verify',
        method: 'post',
        data: payload,
        authRole: false,
      });
    });
  });

  describe('loginBySms', () => {
    it('应调用 POST /v1/app/auth/login/sms', async () => {
      // Arrange
      const payload = {
        phone: '13800138000',
        smsCode: '123456',
        captchaKey: 'key',
        captchaCode: 'code',
      };
      mockRequest.mockResolvedValue('sms-token');

      // Act
      const result = await appAuthService.loginBySms(payload);

      // Assert
      expect(mockRequest).toHaveBeenCalledWith({
        url: '/v1/app/auth/login/sms',
        method: 'post',
        data: payload,
        authRole: false,
      });
      expect(result).toBe('sms-token');
    });
  });

  describe('loginByThirdParty', () => {
    it('应调用 POST /v1/app/auth/login/third-party', async () => {
      // Arrange
      const payload = {
        username: 'wx_user',
        password: '',
        captchaKey: '',
        captchaCode: '',
      };
      mockRequest.mockResolvedValue('third-token');

      // Act
      const result = await appAuthService.loginByThirdParty(payload);

      // Assert
      expect(mockRequest).toHaveBeenCalledWith({
        url: '/v1/app/auth/login/third-party',
        method: 'post',
        data: payload,
        authRole: false,
      });
      expect(result).toBe('third-token');
    });
  });

  describe('logout', () => {
    it('应调用 POST /v1/app/auth/logout 且 authRole 为 user', async () => {
      // Arrange
      mockRequest.mockResolvedValue(undefined);

      // Act
      await appAuthService.logout();

      // Assert
      expect(mockRequest).toHaveBeenCalledWith({
        url: '/v1/app/auth/logout',
        method: 'post',
        authRole: 'user',
      });
    });
  });

  describe('错误处理', () => {
    it('请求失败时应将错误向上抛出', async () => {
      // Arrange
      const error = new Error('网络错误');
      mockRequest.mockRejectedValue(error);

      // Act & Assert
      await expect(appAuthService.getCaptcha()).rejects.toThrow('网络错误');
    });
  });
});
