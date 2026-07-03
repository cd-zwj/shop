package com.payment.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.payment.entity.PlatformUserAuth;
import org.apache.ibatis.annotations.Mapper;

/**
 * 平台用户认证信息表数据访问接口，提供用户登录凭证（手机号/邮箱/第三方等）的增删改查操作。
 * <p>对应实体表：{@link com.payment.entity.PlatformUserAuth}</p>
 */
@Mapper
public interface PlatformUserAuthMapper extends BaseMapper<PlatformUserAuth> {
}
