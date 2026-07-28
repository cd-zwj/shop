package com.payment.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.payment.common.BusinessException;
import com.payment.constant.MerchantPermission;
import com.payment.dto.StoreReviewCreateDTO;
import com.payment.entity.SalesOrder;
import com.payment.entity.Store;
import com.payment.entity.StoreReview;
import com.payment.mapper.SalesOrderMapper;
import com.payment.mapper.StoreMapper;
import com.payment.mapper.StoreReviewMapper;
import com.payment.service.StoreReviewService;
import com.payment.service.MerchantStoreScope;
import com.payment.util.JsonUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;

/** 门店评价最小闭环：订单校验、商家回复、平台可见性处理及评分同步。 */
@Service
@RequiredArgsConstructor
public class StoreReviewServiceImpl implements StoreReviewService {
    private final StoreReviewMapper storeReviewMapper;
    private final SalesOrderMapper salesOrderMapper;
    private final StoreMapper storeMapper;
    private final MerchantStoreScopeService merchantStoreScopeService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public StoreReview create(Long platformUserId, Long tenantId, String orderNo, StoreReviewCreateDTO dto) {
        SalesOrder order = salesOrderMapper.selectOne(new LambdaQueryWrapper<SalesOrder>()
                .eq(SalesOrder::getTenantId, tenantId)
                .eq(SalesOrder::getOrderNo, orderNo)
                .eq(SalesOrder::getDeleted, 0));
        if (order == null || !platformUserId.equals(order.getPlatformUserId())) {
            throw new BusinessException("订单不存在或无权评价");
        }
        if (!"COMPLETED".equals(order.getOrderStatus()) || !"STORE_PICKUP".equals(order.getFulfillmentMode()) || order.getStoreId() == null) {
            throw new BusinessException("订单完成后才能评价门店");
        }
        if (storeReviewMapper.selectCount(new LambdaQueryWrapper<StoreReview>().eq(StoreReview::getOrderId, order.getId())) > 0) {
            throw new BusinessException("该订单已评价");
        }

        StoreReview review = new StoreReview();
        review.setTenantId(tenantId);
        review.setStoreId(order.getStoreId());
        review.setOrderId(order.getId());
        review.setOrderNo(order.getOrderNo());
        review.setPlatformUserId(platformUserId);
        review.setRating(dto.getRating());
        review.setContent(trimToNull(dto.getContent()));
        review.setImageUrlsJson(dto.getImageUrls() == null || dto.getImageUrls().isEmpty() ? null : JsonUtils.toJson(dto.getImageUrls()));
        review.setStatus("VISIBLE");
        try {
            storeReviewMapper.insert(review);
        } catch (DuplicateKeyException exception) {
            throw new BusinessException("该订单已评价");
        }
        refreshStoreRating(order.getStoreId(), tenantId);
        return review;
    }

    @Override
    public StoreReview getMine(Long platformUserId, Long tenantId, String orderNo) {
        return storeReviewMapper.selectOne(new LambdaQueryWrapper<StoreReview>()
                .eq(StoreReview::getTenantId, tenantId)
                .eq(StoreReview::getOrderNo, orderNo)
                .eq(StoreReview::getPlatformUserId, platformUserId));
    }

    @Override
    public Page<StoreReview> listTenantReviews(Long tenantId, Long operatorId, Long storeId, Integer rating, int page, int size) {
        MerchantStoreScope scope = merchantStoreScopeService.resolve(
                tenantId, operatorId, MerchantPermission.ORDER_MANAGE);
        if (storeId != null) {
            merchantStoreScopeService.requireStoreAccess(scope, storeId);
        } else if (!scope.allStores() && scope.storeIds().isEmpty()) {
            return new Page<>(page, size, 0);
        }
        return storeReviewMapper.selectPage(new Page<>(page, size), new LambdaQueryWrapper<StoreReview>()
                .eq(StoreReview::getTenantId, tenantId)
                .eq(storeId != null, StoreReview::getStoreId, storeId)
                .in(storeId == null && !scope.allStores(), StoreReview::getStoreId, scope.storeIds())
                .eq(rating != null, StoreReview::getRating, rating)
                .orderByDesc(StoreReview::getCreateTime));
    }

    @Override
    public Page<StoreReview> listVisibleReviews(Long tenantId, Long storeId, int page, int size) {
        return storeReviewMapper.selectPage(new Page<>(page, size), new LambdaQueryWrapper<StoreReview>()
                .eq(StoreReview::getTenantId, tenantId)
                .eq(StoreReview::getStoreId, storeId)
                .eq(StoreReview::getStatus, "VISIBLE")
                .orderByDesc(StoreReview::getCreateTime));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void reply(Long tenantId, Long reviewId, Long operatorId, String content) {
        StoreReview review = requireReview(reviewId);
        if (!tenantId.equals(review.getTenantId())) {
            throw new BusinessException("评价不存在");
        }
        MerchantStoreScope scope = merchantStoreScopeService.resolve(
                tenantId, operatorId, MerchantPermission.ORDER_MANAGE);
        merchantStoreScopeService.requireStoreAccess(scope, review.getStoreId());
        review.setMerchantReply(content.trim());
        review.setMerchantReplyOperatorId(operatorId);
        review.setMerchantReplyTime(LocalDateTime.now());
        storeReviewMapper.updateById(review);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void moderate(Long reviewId, Long operatorId, boolean visible, String remark) {
        StoreReview review = requireReview(reviewId);
        review.setStatus(visible ? "VISIBLE" : "HIDDEN");
        review.setModerationRemark(remark.trim());
        review.setModerationOperatorId(operatorId);
        review.setModerationTime(LocalDateTime.now());
        storeReviewMapper.updateById(review);
        refreshStoreRating(review.getStoreId(), review.getTenantId());
    }

    private StoreReview requireReview(Long reviewId) {
        StoreReview review = storeReviewMapper.selectById(reviewId);
        if (review == null) {
            throw new BusinessException("评价不存在");
        }
        return review;
    }

    private void refreshStoreRating(Long storeId, Long tenantId) {
        List<StoreReview> visibleReviews = storeReviewMapper.selectList(new LambdaQueryWrapper<StoreReview>()
                .eq(StoreReview::getTenantId, tenantId)
                .eq(StoreReview::getStoreId, storeId)
                .eq(StoreReview::getStatus, "VISIBLE"));
        BigDecimal rating = visibleReviews.isEmpty() ? BigDecimal.ZERO : visibleReviews.stream()
                .map(StoreReview::getRating)
                .map(BigDecimal::valueOf)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .divide(BigDecimal.valueOf(visibleReviews.size()), 2, RoundingMode.HALF_UP);
        Store store = storeMapper.selectById(storeId);
        if (store != null && tenantId.equals(store.getTenantId())) {
            store.setRating(rating);
            storeMapper.updateById(store);
        }
    }

    private String trimToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
