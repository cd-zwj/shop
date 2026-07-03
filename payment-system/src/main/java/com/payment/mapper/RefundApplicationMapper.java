package com.payment.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.payment.entity.RefundApplication;
import org.apache.ibatis.annotations.Mapper;

/**
 * 退款申请表数据访问接口，提供退款申请记录的增删改查操作。
 * <p>对应实体表：{@link com.payment.entity.RefundApplication}</p>
 */
@Mapper
public interface RefundApplicationMapper extends BaseMapper<RefundApplication> {
}
