package com.payment.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.payment.entity.StoreReview;
import org.apache.ibatis.annotations.Mapper;

/** 门店评价数据访问。 */
@Mapper
public interface StoreReviewMapper extends BaseMapper<StoreReview> {
}
