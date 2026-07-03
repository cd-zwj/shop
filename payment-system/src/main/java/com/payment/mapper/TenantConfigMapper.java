package com.payment.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.payment.entity.TenantConfig;
import org.apache.ibatis.annotations.Mapper;

/**
 * 租户配置表数据访问接口，提供租户级别配置项（如支付策略、主题设置等）的增删改查操作。
 * <p>对应实体表：{@link com.payment.entity.TenantConfig}</p>
 */
@Mapper
public interface TenantConfigMapper extends BaseMapper<TenantConfig> {
}
