package com.payment.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.payment.entity.ActivityRule;
import org.apache.ibatis.annotations.Mapper;

/**
 * 营销活动规则数据访问接口。
 */
@Mapper
public interface ActivityRuleMapper extends BaseMapper<ActivityRule> {
}
