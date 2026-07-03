package com.payment.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.payment.entity.MemberLevel;
import org.apache.ibatis.annotations.Mapper;

/**
 * 会员等级表数据访问接口，提供会员等级定义（普通/银卡/金卡等）的增删改查操作。
 * <p>对应实体表：{@link com.payment.entity.MemberLevel}</p>
 */
@Mapper
public interface MemberLevelMapper extends BaseMapper<MemberLevel> {
}
