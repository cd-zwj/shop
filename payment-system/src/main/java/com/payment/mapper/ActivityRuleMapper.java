package com.payment.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.payment.entity.ActivityRule;
import org.apache.ibatis.annotations.Mapper;

/**
 * 营销活动规则数据访问接口，提供营销活动规则表（activity_rule）的 CRUD 操作。
 * 存储活动的触发条件、优惠方式等规则配置。
 */
@Mapper
public interface ActivityRuleMapper extends BaseMapper<ActivityRule> {
}
