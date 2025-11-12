package com.payment.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.payment.entity.UserPoints;
import org.apache.ibatis.annotations.Mapper;

/**
 * 用户积分Mapper
 */
@Mapper
public interface UserPointsMapper extends BaseMapper<UserPoints> {
}
