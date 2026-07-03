package com.payment.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.payment.entity.PlatformAuthProvider;
import org.apache.ibatis.annotations.Mapper;

/**
 * 第三方认证提供方表数据访问接口，提供第三方登录方式（微信/支付宝/GitHub等）配置的增删改查操作。
 * <p>对应实体表：{@link com.payment.entity.PlatformAuthProvider}</p>
 */
@Mapper
public interface PlatformAuthProviderMapper extends BaseMapper<PlatformAuthProvider> {
}
