package com.payment.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.payment.entity.CouponWriteOffRecord;
import org.apache.ibatis.annotations.Mapper;

/**
 * 优惠券核销记录数据访问接口。
 */
@Mapper
public interface CouponWriteOffRecordMapper extends BaseMapper<CouponWriteOffRecord> {
}
