package com.payment.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.payment.entity.CouponScope;
import org.apache.ibatis.annotations.Mapper;

/**
 * 优惠券适用范围数据访问接口。
 */
@Mapper
public interface CouponScopeMapper extends BaseMapper<CouponScope> {
}
