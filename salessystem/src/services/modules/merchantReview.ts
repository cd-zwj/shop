import { request } from '../request';
import type { PageResult } from '../../types/api';
import type { StoreReview } from '../../types/review';

export const merchantReviewService = {
  list(tenantId: number, storeId?: number, rating?: number, pageNum = 1, pageSize = 20) {
    return request<PageResult<StoreReview>>({
      url: `/v1/merchant/tenants/${tenantId}/reviews`,
      method: 'get',
      params: { storeId, rating, pageNum, pageSize },
      authRole: 'merchant',
    });
  },
  reply(tenantId: number, reviewId: number, content: string) {
    return request<void>({
      url: `/v1/merchant/tenants/${tenantId}/reviews/${reviewId}/reply`,
      method: 'put',
      data: { content },
      authRole: 'merchant',
    });
  },
};
