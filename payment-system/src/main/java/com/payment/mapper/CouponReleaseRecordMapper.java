package com.payment.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.payment.entity.CouponReleaseRecord;
import org.apache.ibatis.annotations.Mapper;

/**
 * 优惠券释放记录数据访问接口。
 */
@Mapper
public interface CouponReleaseRecordMapper extends BaseMapper<CouponReleaseRecord> {
}
