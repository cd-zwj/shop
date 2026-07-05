import { describe, it, expect, vi, beforeEach } from 'vitest';

const mockRequest = vi.fn();
vi.mock('../request', () => ({
  request: (...args: unknown[]) => mockRequest(...args),
}));

import { appOrderService } from './appOrder';

beforeEach(() => {
  vi.clearAllMocks();
});

describe('appOrderService', () => {
  describe('createOrder', () => {
    it('应调用 POST /v1/app/orders 并携带订单数据', async () => {
      // Arrange
      const payload = { tenantId: 1, items: [{ productId: 10, quantity: 2 }] };
      const payment = { billNo: 'BILL001', payUrl: 'https://pay.example.com' };
      mockRequest.mockResolvedValue(payment);

      // Act
      const result = await appOrderService.createOrder(payload as any);

      // Assert
      expect(mockRequest).toHaveBeenCalledWith({
        url: '/v1/app/orders',
        method: 'post',
        data: payload,
        authRole: 'user',
      });
      expect(result).toEqual(payment);
    });
  });

  describe('listOrders', () => {
    it('应调用 GET /v1/app/orders 并传入默认分页参数', async () => {
      // Arrange
      const pageResult = { records: [], total: 0, size: 10, current: 1, pages: 0 };
      mockRequest.mockResolvedValue(pageResult);

      // Act
      const result = await appOrderService.listOrders();

      // Assert
      expect(mockRequest).toHaveBeenCalledWith({
        url: '/v1/app/orders',
        method: 'get',
        params: { current: 1, size: 10 },
        authRole: 'user',
      });
      expect(result).toEqual(pageResult);
    });

    it('应支持自定义分页参数', async () => {
      // Arrange
      mockRequest.mockResolvedValue({ records: [], total: 0, size: 20, current: 3, pages: 0 });

      // Act
      await appOrderService.listOrders(3, 20);

      // Assert
      expect(mockRequest).toHaveBeenCalledWith(
        expect.objectContaining({ params: { current: 3, size: 20 } }),
      );
    });
  });

  describe('getOrder', () => {
    it('应调用 GET /v1/app/orders/:orderNo 并适配扁平详情响应', async () => {
      // Arrange
      const orderDetail = {
        orderNo: 'ORD001',
        tenantId: 1,
        platformUserId: 2,
        orderStatus: 'PAID',
        payStatus: 'SUCCESS',
        totalAmount: 1000,
        items: [],
        paymentBillNo: 'PB001',
      };
      mockRequest.mockResolvedValue(orderDetail);

      // Act
      const result = await appOrderService.getOrder('ORD001');

      // Assert
      expect(mockRequest).toHaveBeenCalledWith({
        url: '/v1/app/orders/ORD001',
        method: 'get',
        authRole: 'user',
      });
      expect(result.order.orderNo).toBe('ORD001');
      expect(result.order.tenantId).toBe(1);
      expect(result.items).toEqual([]);
      expect(result.paymentBillNo).toBe('PB001');
    });
  });

  describe('repayOrder', () => {
    it('应调用 POST /v1/app/orders/:orderNo/repay 默认支付宝渠道', async () => {
      // Arrange
      const payment = { billNo: 'BILL002', payUrl: 'https://pay.example.com/2' };
      mockRequest.mockResolvedValue(payment);

      // Act
      const result = await appOrderService.repayOrder('ORD001');

      // Assert
      expect(mockRequest).toHaveBeenCalledWith({
        url: '/v1/app/orders/ORD001/repay',
        method: 'post',
        params: { paymentChannelCode: 'ALIPAY_PAGE' },
        authRole: 'user',
      });
      expect(result).toEqual(payment);
    });

    it('应支持指定 EXT_PROVIDER 渠道', async () => {
      // Arrange
      mockRequest.mockResolvedValue({});

      // Act
      await appOrderService.repayOrder('ORD001', 'EXT_PROVIDER');

      // Assert
      expect(mockRequest).toHaveBeenCalledWith(
        expect.objectContaining({ params: { paymentChannelCode: 'EXT_PROVIDER' } }),
      );
    });
  });

  describe('cancelOrder', () => {
    it('应调用 POST /v1/app/orders/:orderNo/cancel', async () => {
      // Arrange
      mockRequest.mockResolvedValue(undefined);

      // Act
      await appOrderService.cancelOrder('ORD001');

      // Assert
      expect(mockRequest).toHaveBeenCalledWith({
        url: '/v1/app/orders/ORD001/cancel',
        method: 'post',
        authRole: 'user',
      });
    });
  });

  describe('错误处理', () => {
    it('创建订单失败时应将错误向上抛出', async () => {
      // Arrange
      const error = new Error('库存不足');
      mockRequest.mockRejectedValue(error);

      // Act & Assert
      await expect(
        appOrderService.createOrder({} as any),
      ).rejects.toThrow('库存不足');
    });
  });
});
