package com.payment.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.payment.entity.CouponTemplate;
import org.apache.ibatis.annotations.Mapper;

/**
 * 优惠券模板表数据访问接口，提供优惠券模板的增删改查操作。
 * <p>对应实体表：{@link com.payment.entity.CouponTemplate}</p>
 */
@Mapper
public interface CouponTemplateMapper extends BaseMapper<CouponTemplate> {
}
