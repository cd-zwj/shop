package com.payment.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.payment.entity.TenantEmployee;
import org.apache.ibatis.annotations.Mapper;

/**
 * 租户员工表数据访问接口，提供租户下商户端员工（B端用户）的增删改查操作。
 * <p>对应实体表：{@link com.payment.entity.TenantEmployee}</p>
 */
@Mapper
public interface TenantEmployeeMapper extends BaseMapper<TenantEmployee> {
}
