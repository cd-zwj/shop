package com.payment.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.payment.entity.PlatformAuthProvider;
import org.apache.ibatis.annotations.Mapper;

/**
 * 第三方登录方式数据访问接口，用于执行第三方登录方式数据的增删改查。
 */
@Mapper
public interface PlatformAuthProviderMapper extends BaseMapper<PlatformAuthProvider> {
}
