package com.payment.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.payment.entity.UserPoints;
import org.apache.ibatis.annotations.Mapper;

/**
 * 用户积分数据访问接口，提供用户积分表（user_points）的 CRUD 操作。
 * 管理用户积分账户的积分余额、积分获取与消耗记录。
 */
@Mapper
public interface UserPointsMapper extends BaseMapper<UserPoints> {
}
