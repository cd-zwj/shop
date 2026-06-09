package com.payment.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.payment.entity.MemberGrowthLog;
import org.apache.ibatis.annotations.Mapper;

/**
 * 会员成长值日志数据访问接口。
 */
@Mapper
public interface MemberGrowthLogMapper extends BaseMapper<MemberGrowthLog> {
}
