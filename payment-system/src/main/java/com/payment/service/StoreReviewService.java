package com.payment.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.payment.dto.StoreReviewCreateDTO;
import com.payment.entity.StoreReview;

/** 已完成订单的门店评价与回复服务。 */
public interface StoreReviewService {
    StoreReview create(Long platformUserId, Long tenantId, String orderNo, StoreReviewCreateDTO dto);
    StoreReview getMine(Long platformUserId, Long tenantId, String orderNo);
    Page<StoreReview> listTenantReviews(Long tenantId, Long storeId, Integer rating, int page, int size);
    Page<StoreReview> listVisibleReviews(Long tenantId, Long storeId, int page, int size);
    void reply(Long tenantId, Long reviewId, Long operatorId, String content);
    void moderate(Long reviewId, Long operatorId, boolean visible, String remark);
}
