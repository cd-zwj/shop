import { request } from '../request';
import type { PageResult } from '../../types/api';
import type { StoreReview, StoreReviewCreatePayload } from '../../types/review';

export const appReviewService = {
  create(tenantId: number, orderNo: string, payload: StoreReviewCreatePayload) {
    return request<StoreReview>({
      url: `/v1/app/tenants/${tenantId}/orders/${orderNo}/review`,
      method: 'post',
      data: payload,
      authRole: 'user',
    });
  },
  getMine(tenantId: number, orderNo: string) {
    return request<StoreReview | null>({
      url: `/v1/app/tenants/${tenantId}/orders/${orderNo}/review`,
      method: 'get',
      authRole: 'user',
    });
  },
  listStoreReviews(tenantId: number, storeId: number, pageNum = 1, pageSize = 10) {
    return request<PageResult<StoreReview>>({
      url: `/v1/app/tenants/${tenantId}/stores/${storeId}/reviews`,
      method: 'get',
      params: { pageNum, pageSize },
      authRole: 'user',
    });
  },
};
