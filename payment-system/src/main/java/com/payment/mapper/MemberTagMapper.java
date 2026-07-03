package com.payment.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.payment.entity.MemberTag;
import org.apache.ibatis.annotations.Mapper;

/**
 * 会员标签定义表数据访问接口，提供会员标签（如VIP、高频用户等）定义的增删改查操作。
 * <p>对应实体表：{@link com.payment.entity.MemberTag}</p>
 */
@Mapper
public interface MemberTagMapper extends BaseMapper<MemberTag> {
}
