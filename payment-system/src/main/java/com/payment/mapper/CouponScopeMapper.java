package com.payment.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.payment.entity.CouponScope;
import org.apache.ibatis.annotations.Mapper;

/**
 * 优惠券适用范围表数据访问接口，提供优惠券与商品/品类/商户关联关系的增删改查操作。
 * <p>对应实体表：{@link com.payment.entity.CouponScope}</p>
 */
@Mapper
public interface CouponScopeMapper extends BaseMapper<CouponScope> {
}
