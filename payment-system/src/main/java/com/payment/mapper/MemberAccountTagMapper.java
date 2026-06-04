package com.payment.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.payment.entity.MemberAccountTag;
import org.apache.ibatis.annotations.Mapper;

/**
 * 会员标签关联数据访问接口。
 */
@Mapper
public interface MemberAccountTagMapper extends BaseMapper<MemberAccountTag> {
}
