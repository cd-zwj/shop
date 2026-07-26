package com.payment.vo;

import com.fasterxml.jackson.core.type.TypeReference;
import com.payment.entity.StoreReview;
import com.payment.util.JsonUtils;
import lombok.Builder;
import lombok.Data;

import java.util.List;

/** 门店评价对外视图。 */
@Data
@Builder
public class StoreReviewVO {
    private Long id;
    private Long storeId;
    private String orderNo;
    private Integer rating;
    private String content;
    private List<String> imageUrls;
    private String merchantReply;
    private String merchantReplyTime;
    private String status;
    private String moderationRemark;
    private String createTime;

    public static StoreReviewVO from(StoreReview review) {
        if (review == null) {
            return null;
        }
        List<String> imageUrls = review.getImageUrlsJson() == null || review.getImageUrlsJson().isBlank()
                ? List.of()
                : JsonUtils.fromJson(review.getImageUrlsJson(), new TypeReference<List<String>>() { });
        return StoreReviewVO.builder()
                .id(review.getId())
                .storeId(review.getStoreId())
                .orderNo(review.getOrderNo())
                .rating(review.getRating())
                .content(review.getContent())
                .imageUrls(imageUrls)
                .merchantReply(review.getMerchantReply())
                .merchantReplyTime(VoConverterUtil.formatTime(review.getMerchantReplyTime()))
                .status(review.getStatus())
                .moderationRemark(review.getModerationRemark())
                .createTime(VoConverterUtil.formatTime(review.getCreateTime()))
                .build();
    }
}
