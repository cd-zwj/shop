package com.payment.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.payment.entity.MemberTag;
import org.apache.ibatis.annotations.Mapper;

/**
 * 会员标签数据访问接口。
 */
@Mapper
public interface MemberTagMapper extends BaseMapper<MemberTag> {
}
