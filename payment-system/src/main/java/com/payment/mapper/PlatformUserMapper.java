package com.payment.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.payment.entity.PlatformUser;
import org.apache.ibatis.annotations.Mapper;

/**
 * 平台用户表数据访问接口，提供全局平台用户（C端用户）的增删改查操作。
 * <p>对应实体表：{@link com.payment.entity.PlatformUser}</p>
 */
@Mapper
public interface PlatformUserMapper extends BaseMapper<PlatformUser> {
}
