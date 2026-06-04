package com.payment.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.payment.entity.CouponTemplate;
import org.apache.ibatis.annotations.Mapper;

/**
 * 优惠券模板数据访问接口。
 */
@Mapper
public interface CouponTemplateMapper extends BaseMapper<CouponTemplate> {
}
