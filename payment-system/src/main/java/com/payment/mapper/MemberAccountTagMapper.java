package com.payment.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.payment.entity.MemberAccountTag;
import org.apache.ibatis.annotations.Mapper;

/**
 * 会员-标签关联表数据访问接口，提供会员与标签多对多关联关系的增删改查操作。
 * <p>对应实体表：{@link com.payment.entity.MemberAccountTag}</p>
 */
@Mapper
public interface MemberAccountTagMapper extends BaseMapper<MemberAccountTag> {
}
