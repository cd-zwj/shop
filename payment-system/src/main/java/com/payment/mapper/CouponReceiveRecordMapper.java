package com.payment.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.payment.entity.CouponReceiveRecord;
import org.apache.ibatis.annotations.Mapper;

/**
 * 优惠券领取记录数据访问接口。
 */
@Mapper
public interface CouponReceiveRecordMapper extends BaseMapper<CouponReceiveRecord> {
}
