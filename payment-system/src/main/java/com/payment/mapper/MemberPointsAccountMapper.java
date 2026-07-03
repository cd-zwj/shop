package com.payment.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.payment.entity.MemberPointsAccount;
import org.apache.ibatis.annotations.Mapper;

/**
 * 会员积分账户表数据访问接口，提供会员积分余额账户的增删改查操作。
 * <p>对应实体表：{@link com.payment.entity.MemberPointsAccount}</p>
 */
@Mapper
public interface MemberPointsAccountMapper extends BaseMapper<MemberPointsAccount> {
}
