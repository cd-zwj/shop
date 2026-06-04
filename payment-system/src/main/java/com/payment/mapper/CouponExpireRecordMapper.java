package com.payment.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.payment.entity.CouponExpireRecord;
import org.apache.ibatis.annotations.Mapper;

/**
 * 优惠券过期记录数据访问接口。
 */
@Mapper
public interface CouponExpireRecordMapper extends BaseMapper<CouponExpireRecord> {
}
