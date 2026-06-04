package com.payment.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.payment.entity.MemberLevel;
import org.apache.ibatis.annotations.Mapper;

/**
 * 会员等级数据访问接口。
 */
@Mapper
public interface MemberLevelMapper extends BaseMapper<MemberLevel> {
}
