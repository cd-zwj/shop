package com.payment.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.payment.entity.PointsLog;
import org.apache.ibatis.annotations.Mapper;

/**
 * 积分明细表数据访问接口，提供积分收支明细记录的增删改查操作。
 * <p>对应实体表：{@link com.payment.entity.PointsLog}</p>
 */
@Mapper
public interface PointsLogMapper extends BaseMapper<PointsLog> {
}
