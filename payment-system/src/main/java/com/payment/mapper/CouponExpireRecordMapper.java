package com.payment.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.payment.entity.CouponExpireRecord;
import org.apache.ibatis.annotations.Mapper;

/**
 * 优惠券过期记录表数据访问接口，提供优惠券过期处理记录的增删改查操作。
 * <p>对应实体表：{@link com.payment.entity.CouponExpireRecord}</p>
 */
@Mapper
public interface CouponExpireRecordMapper extends BaseMapper<CouponExpireRecord> {
}
