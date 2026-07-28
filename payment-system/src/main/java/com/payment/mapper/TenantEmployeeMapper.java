package com.payment.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.payment.entity.TenantEmployee;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 租户员工表数据访问接口，提供租户下商户端员工（B端用户）的增删改查操作。
 * <p>对应实体表：{@link com.payment.entity.TenantEmployee}</p>
 */
@Mapper
public interface TenantEmployeeMapper extends BaseMapper<TenantEmployee> {

    /** 锁定租户的全部启用 OWNER，串行化“最后一个 OWNER”校验。 */
    @Select("""
            SELECT * FROM tenant_employee
            WHERE tenant_id = #{tenantId}
              AND employee_role = 'OWNER'
              AND status = 1
            ORDER BY id
            FOR UPDATE
            """)
    List<TenantEmployee> selectActiveOwnersForUpdate(@Param("tenantId") Long tenantId);
}
