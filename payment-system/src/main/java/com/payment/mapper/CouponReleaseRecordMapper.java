package com.payment.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.payment.entity.CouponReleaseRecord;
import org.apache.ibatis.annotations.Mapper;

/**
 * 优惠券释放记录表数据访问接口，提供订单取消后优惠券回退释放记录的增删改查操作。
 * <p>对应实体表：{@link com.payment.entity.CouponReleaseRecord}</p>
 */
@Mapper
public interface CouponReleaseRecordMapper extends BaseMapper<CouponReleaseRecord> {
}
