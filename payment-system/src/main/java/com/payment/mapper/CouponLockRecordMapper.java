package com.payment.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.payment.entity.CouponLockRecord;
import org.apache.ibatis.annotations.Mapper;

/**
 * 优惠券锁定记录表数据访问接口，提供下单时优惠券锁定状态的增删改查操作。
 * <p>对应实体表：{@link com.payment.entity.CouponLockRecord}</p>
 */
@Mapper
public interface CouponLockRecordMapper extends BaseMapper<CouponLockRecord> {
}
