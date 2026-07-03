package com.payment.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.payment.entity.UserBehaviorLog;
import org.apache.ibatis.annotations.Mapper;

/**
 * 用户行为日志数据访问接口，提供用户行为日志表（user_behavior_log）的 CRUD 操作。
 * 记录用户在平台上的浏览、点击、收藏等行为数据，用于数据分析和推荐。
 */
@Mapper
public interface UserBehaviorLogMapper extends BaseMapper<UserBehaviorLog> {
}

