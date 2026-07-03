package com.payment.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.payment.entity.CouponReceiveRecord;
import org.apache.ibatis.annotations.Mapper;

/**
 * 优惠券领取记录表数据访问接口，提供用户领取优惠券记录的增删改查操作。
 * <p>对应实体表：{@link com.payment.entity.CouponReceiveRecord}</p>
 */
@Mapper
public interface CouponReceiveRecordMapper extends BaseMapper<CouponReceiveRecord> {
}
