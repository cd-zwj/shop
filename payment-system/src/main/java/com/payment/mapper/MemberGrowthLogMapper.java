package com.payment.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.payment.entity.MemberGrowthLog;
import org.apache.ibatis.annotations.Mapper;

/**
 * 会员成长值日志表数据访问接口，提供成长值变动流水记录的增删改查操作。
 * <p>对应实体表：{@link com.payment.entity.MemberGrowthLog}</p>
 */
@Mapper
public interface MemberGrowthLogMapper extends BaseMapper<MemberGrowthLog> {
}
