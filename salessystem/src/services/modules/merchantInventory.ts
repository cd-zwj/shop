import { request } from '../request';
import type { PageResult } from '../../types/api';
import type {
  MerchantStoreInventory,
  MerchantStoreInventoryAdjustment,
  MerchantStoreInventoryLog,
} from '../../types/merchant';

export const merchantInventoryService = {
  listStocks(
    tenantId: number,
    filters: {
      current?: number;
      size?: number;
      storeId?: number;
      productId?: number;
      lowStockOnly?: boolean;
      threshold?: number;
    } = {},
  ) {
    return request<PageResult<MerchantStoreInventory>>({
      url: `/v1/merchant/tenants/${tenantId}/inventory`,
      method: 'get',
      params: {
        current: filters.current ?? 1,
        size: filters.size ?? 100,
        storeId: filters.storeId,
        productId: filters.productId,
        lowStockOnly: filters.lowStockOnly || undefined,
        threshold: filters.threshold,
      },
      authRole: 'merchant',
    });
  },

  adjustStock(tenantId: number, payload: MerchantStoreInventoryAdjustment) {
    return request<MerchantStoreInventory>({
      url: `/v1/merchant/tenants/${tenantId}/inventory/adjustments`,
      method: 'post',
      data: payload,
      authRole: 'merchant',
    });
  },

  listLogs(
    tenantId: number,
    filters: { current?: number; size?: number; storeId?: number; productId?: number } = {},
  ) {
    return request<PageResult<MerchantStoreInventoryLog>>({
      url: `/v1/merchant/tenants/${tenantId}/inventory/logs`,
      method: 'get',
      params: {
        current: filters.current ?? 1,
        size: filters.size ?? 50,
        storeId: filters.storeId,
        productId: filters.productId,
      },
      authRole: 'merchant',
    });
  },
};
