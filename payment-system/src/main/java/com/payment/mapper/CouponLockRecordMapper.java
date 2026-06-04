package com.payment.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.payment.entity.CouponLockRecord;
import org.apache.ibatis.annotations.Mapper;

/**
 * 优惠券锁定记录数据访问接口。
 */
@Mapper
public interface CouponLockRecordMapper extends BaseMapper<CouponLockRecord> {
}
