import { request } from '../request';
import type { PageResult } from '../../types/api';

/**
 * 商品类型 - 决定交付方式与前端如何渲染 payload
 */
export type ProductType =
  | 'PHYSICAL'
  | 'VIRTUAL'
  | 'CARD_KEY'
  | 'SERVICE'
  | 'SUBSCRIPTION';

export type DeliveryStatus =
  | 'PENDING'
  | 'DELIVERING'
  | 'DELIVERED'
  | 'CONFIRMED'
  | 'FAILED'
  | 'REVOKED';

export interface PurchaseRecord {
  id: number;
  orderId: number;
  orderNo: string;
  orderItemId: number;
  productId: number;
  productType: ProductType;
  status: DeliveryStatus;
  /** JSON 字符串，按 productType 解读 */
  payload?: string | null;
  failReason?: string | null;
  deliveredTime?: string | null;
  confirmedTime?: string | null;
  expireTime?: string | null;
  createTime: string;
}

export const appPurchasesService = {
  /** 列表 - status 可选筛选 */
  list(status?: DeliveryStatus, pageNum = 1, pageSize = 10) {
    return request<PageResult<PurchaseRecord>>({
      url: '/v1/app/purchases',
      method: 'get',
      params: { status, current: pageNum, size: pageSize },
      authRole: 'user',
    });
  },

  detail(id: number) {
    return request<PurchaseRecord>({
      url: `/v1/app/purchases/${id}`,
      method: 'get',
      authRole: 'user',
    });
  },

  /** 用户确认收货 / 已使用 */
  confirm(id: number) {
    return request<PurchaseRecord>({
      url: `/v1/app/purchases/${id}/confirm`,
      method: 'post',
      authRole: 'user',
    });
  },
};

export default appPurchasesService;
