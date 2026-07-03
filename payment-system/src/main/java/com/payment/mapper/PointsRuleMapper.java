package com.payment.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.payment.entity.PointsRule;
import org.apache.ibatis.annotations.Mapper;

/**
 * 积分规则表数据访问接口，提供积分获取与消耗规则配置的增删改查操作。
 * <p>对应实体表：{@link com.payment.entity.PointsRule}</p>
 */
@Mapper
public interface PointsRuleMapper extends BaseMapper<PointsRule> {
}
