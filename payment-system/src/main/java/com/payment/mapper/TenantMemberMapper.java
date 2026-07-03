package com.payment.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.payment.entity.TenantMember;
import org.apache.ibatis.annotations.Mapper;

/**
 * 租户会员表数据访问接口，提供租户下C端会员信息的增删改查操作。
 * <p>对应实体表：{@link com.payment.entity.TenantMember}</p>
 */
@Mapper
public interface TenantMemberMapper extends BaseMapper<TenantMember> {
}
