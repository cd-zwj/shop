import { describe, it, expect, vi, beforeEach } from 'vitest';

const mockRequest = vi.fn();
vi.mock('../request', () => ({
  request: (...args: unknown[]) => mockRequest(...args),
}));

import { appWalletService } from './appWallet';

beforeEach(() => {
  vi.clearAllMocks();
});

describe('appWalletService', () => {
  describe('getUnifiedWallet', () => {
    it('应调用 GET /v1/app/wallets/unified', async () => {
      // Arrange
      const wallet = { balance: 1000, frozenAmount: 0 };
      mockRequest.mockResolvedValue(wallet);

      // Act
      const result = await appWalletService.getUnifiedWallet();

      // Assert
      expect(mockRequest).toHaveBeenCalledWith({
        url: '/v1/app/wallets/unified',
        method: 'get',
        authRole: 'user',
      });
      expect(result).toEqual(wallet);
    });
  });

  describe('getUnifiedWalletLogs', () => {
    it('应调用 GET /v1/app/wallets/unified/logs 并传入默认分页参数', async () => {
      // Arrange
      const logs = { records: [], total: 0, size: 10, current: 1, pages: 0 };
      mockRequest.mockResolvedValue(logs);

      // Act
      const result = await appWalletService.getUnifiedWalletLogs();

      // Assert
      expect(mockRequest).toHaveBeenCalledWith({
        url: '/v1/app/wallets/unified/logs',
        method: 'get',
        params: { current: 1, size: 10 },
        authRole: 'user',
      });
      expect(result).toEqual(logs);
    });
  });

  describe('listTenantAssetSummaries', () => {
    it('应调用 GET /v1/app/assets/tenant-summaries', async () => {
      // Arrange
      const summaries = [{ tenantId: 1, tenantName: '咖啡店', points: 120 }];
      mockRequest.mockResolvedValue(summaries);

      // Act
      const result = await appWalletService.listTenantAssetSummaries();

      // Assert
      expect(mockRequest).toHaveBeenCalledWith({
        url: '/v1/app/assets/tenant-summaries',
        method: 'get',
        authRole: 'user',
      });
      expect(result).toEqual(summaries);
    });
  });

  describe('createUnifiedRecharge', () => {
    it('应调用 POST /v1/app/wallets/unified/recharges', async () => {
      // Arrange
      const payload = { amount: 500, paymentChannelCode: 'ALIPAY_PAGE' as const };
      const payment = { billNo: 'BILL003', payUrl: 'https://pay.example.com/3' };
      mockRequest.mockResolvedValue(payment);

      // Act
      const result = await appWalletService.createUnifiedRecharge(payload as any);

      // Assert
      expect(mockRequest).toHaveBeenCalledWith({
        url: '/v1/app/wallets/unified/recharges',
        method: 'post',
        data: payload,
        authRole: 'user',
      });
      expect(result).toEqual(payment);
    });
  });

  describe('listUnifiedRechargeRules', () => {
    it('应调用 GET /v1/app/wallets/unified/recharge-rules', async () => {
      // Arrange
      const rules = [{ id: 1, amount: 100, bonus: 10 }];
      mockRequest.mockResolvedValue(rules);

      // Act
      const result = await appWalletService.listUnifiedRechargeRules();

      // Assert
      expect(mockRequest).toHaveBeenCalledWith({
        url: '/v1/app/wallets/unified/recharge-rules',
        method: 'get',
        authRole: 'user',
      });
      expect(result).toEqual(rules);
    });
  });

  describe('getMerchantWallet', () => {
    it('应调用 GET /v1/app/tenants/:tenantId/wallet', async () => {
      // Arrange
      const wallet = { balance: 5000, frozenAmount: 100 };
      mockRequest.mockResolvedValue(wallet);

      // Act
      const result = await appWalletService.getMerchantWallet(42);

      // Assert
      expect(mockRequest).toHaveBeenCalledWith({
        url: '/v1/app/tenants/42/wallet',
        method: 'get',
        authRole: 'user',
      });
      expect(result).toEqual(wallet);
    });
  });

  describe('getMerchantWalletLogs', () => {
    it('应调用 GET /v1/app/tenants/:tenantId/wallet/logs 并传入分页', async () => {
      // Arrange
      const logs = { records: [], total: 0, size: 10, current: 1, pages: 0 };
      mockRequest.mockResolvedValue(logs);

      // Act
      await appWalletService.getMerchantWalletLogs(42);

      // Assert
      expect(mockRequest).toHaveBeenCalledWith({
        url: '/v1/app/tenants/42/wallet/logs',
        method: 'get',
        params: { current: 1, size: 10 },
        authRole: 'user',
      });
    });
  });

  describe('listMerchantRechargeRules', () => {
    it('应调用 GET /v1/app/tenants/:tenantId/recharge-rules', async () => {
      // Arrange
      const rules = [{ id: 1, amount: 200 }];
      mockRequest.mockResolvedValue(rules);

      // Act
      const result = await appWalletService.listMerchantRechargeRules(42);

      // Assert
      expect(mockRequest).toHaveBeenCalledWith({
        url: '/v1/app/tenants/42/recharge-rules',
        method: 'get',
        authRole: 'user',
      });
      expect(result).toEqual(rules);
    });
  });

  describe('createMerchantRecharge', () => {
    it('应调用 POST /v1/app/tenants/:tenantId/wallet/recharges', async () => {
      // Arrange
      const payload = { amount: 300, paymentChannelCode: 'ALIPAY_PAGE' as const };
      const payment = { billNo: 'BILL004' };
      mockRequest.mockResolvedValue(payment);

      // Act
      const result = await appWalletService.createMerchantRecharge(42, payload as any);

      // Assert
      expect(mockRequest).toHaveBeenCalledWith({
        url: '/v1/app/tenants/42/wallet/recharges',
        method: 'post',
        data: payload,
        authRole: 'user',
      });
      expect(result).toEqual(payment);
    });
  });

  describe('getPointsAccount', () => {
    it('应调用 GET /v1/app/tenants/:tenantId/points', async () => {
      // Arrange
      const account = { totalPoints: 500, availablePoints: 450 };
      mockRequest.mockResolvedValue(account);

      // Act
      const result = await appWalletService.getPointsAccount(42);

      // Assert
      expect(mockRequest).toHaveBeenCalledWith({
        url: '/v1/app/tenants/42/points',
        method: 'get',
        authRole: 'user',
      });
      expect(result).toEqual(account);
    });
  });

  describe('getPointsLogs', () => {
    it('应调用 GET /v1/app/tenants/:tenantId/points/logs 并传入分页', async () => {
      // Arrange
      const logs = { records: [], total: 0, size: 10, current: 1, pages: 0 };
      mockRequest.mockResolvedValue(logs);

      // Act
      await appWalletService.getPointsLogs(42, 2, 20);

      // Assert
      expect(mockRequest).toHaveBeenCalledWith({
        url: '/v1/app/tenants/42/points/logs',
        method: 'get',
        params: { current: 2, size: 20 },
        authRole: 'user',
      });
    });
  });

  describe('错误处理', () => {
    it('获取钱包失败时应将错误向上抛出', async () => {
      // Arrange
      mockRequest.mockRejectedValue(new Error('服务不可用'));

      // Act & Assert
      await expect(appWalletService.getUnifiedWallet()).rejects.toThrow('服务不可用');
    });
  });
});
