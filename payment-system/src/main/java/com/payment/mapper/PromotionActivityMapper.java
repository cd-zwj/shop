package com.payment.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.payment.entity.PromotionActivity;
import org.apache.ibatis.annotations.Mapper;

/**
 * 营销活动数据访问接口，提供营销活动表（promotion_activity）的 CRUD 操作。
 * 管理满减、折扣、赠品等各类营销活动的生命周期。
 */
@Mapper
public interface PromotionActivityMapper extends BaseMapper<PromotionActivity> {
}
