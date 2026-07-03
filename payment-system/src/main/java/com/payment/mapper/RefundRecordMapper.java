package com.payment.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.payment.entity.RefundRecord;
import org.apache.ibatis.annotations.Mapper;

/**
 * 退款记录表数据访问接口，提供退款流水记录的增删改查操作。
 * <p>对应实体表：{@link com.payment.entity.RefundRecord}</p>
 */
@Mapper
public interface RefundRecordMapper extends BaseMapper<RefundRecord> {
}
