import { describe, it, expect, vi, beforeEach } from 'vitest';

const mockRequest = vi.fn();
vi.mock('../request', () => ({
  request: (...args: unknown[]) => mockRequest(...args),
}));

import { appCatalogService } from './appCatalog';

beforeEach(() => {
  vi.clearAllMocks();
});

describe('appCatalogService', () => {
  describe('listTenants', () => {
    it('应调用 GET /v1/app/tenants 且 authRole 为 false', async () => {
      // Arrange
      const tenants = [{ id: 1, name: '商户A' }, { id: 2, name: '商户B' }];
      mockRequest.mockResolvedValue(tenants);

      // Act
      const result = await appCatalogService.listTenants();

      // Assert
      expect(mockRequest).toHaveBeenCalledWith({
        url: '/v1/app/tenants',
        method: 'get',
        authRole: false,
      });
      expect(result).toEqual(tenants);
    });
  });

  describe('getTenant', () => {
    it('应调用 GET /v1/app/tenants/:tenantId', async () => {
      // Arrange
      const tenant = { id: 42, name: '商户A', description: '测试商户' };
      mockRequest.mockResolvedValue(tenant);

      // Act
      const result = await appCatalogService.getTenant(42);

      // Assert
      expect(mockRequest).toHaveBeenCalledWith({
        url: '/v1/app/tenants/42',
        method: 'get',
        authRole: false,
      });
      expect(result).toEqual(tenant);
    });
  });

  describe('listTenantProducts', () => {
    it('应调用 GET /v1/app/tenants/:tenantId/products', async () => {
      // Arrange
      const products = [{ id: 1, name: '商品1' }, { id: 2, name: '商品2' }];
      mockRequest.mockResolvedValue(products);

      // Act
      const result = await appCatalogService.listTenantProducts(42);

      // Assert
      expect(mockRequest).toHaveBeenCalledWith({
        url: '/v1/app/tenants/42/products',
        method: 'get',
        authRole: false,
      });
      expect(result).toEqual(products);
    });
  });

  describe('getProduct', () => {
    it('应调用 GET /v1/app/products/:productId', async () => {
      // Arrange
      const product = { id: 100, name: '测试商品', price: 99.9 };
      mockRequest.mockResolvedValue(product);

      // Act
      const result = await appCatalogService.getProduct(100);

      // Assert
      expect(mockRequest).toHaveBeenCalledWith({
        url: '/v1/app/products/100',
        method: 'get',
        authRole: false,
      });
      expect(result).toEqual(product);
    });
  });

  describe('错误处理', () => {
    it('获取商户列表失败时应将错误向上抛出', async () => {
      // Arrange
      mockRequest.mockRejectedValue(new Error('网络超时'));

      // Act & Assert
      await expect(appCatalogService.listTenants()).rejects.toThrow('网络超时');
    });
  });
});
