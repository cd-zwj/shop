package com.payment.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.payment.entity.MemberPointsLog;
import org.apache.ibatis.annotations.Mapper;

/**
 * 会员积分流水日志表数据访问接口，提供积分获取、消耗、过期等流水记录的增删改查操作。
 * <p>对应实体表：{@link com.payment.entity.MemberPointsLog}</p>
 */
@Mapper
public interface MemberPointsLogMapper extends BaseMapper<MemberPointsLog> {
}
