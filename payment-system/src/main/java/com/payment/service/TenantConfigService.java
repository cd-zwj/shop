package com.payment.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.payment.entity.TenantConfig;
import com.payment.vo.TenantConfigVO;

import java.util.List;

/**
 * 租户配置服务接口
 */
public interface TenantConfigService extends IService<TenantConfig> {

    /**
     * 按 key 获取单个配置，不存在时创建默认值
     */
    TenantConfigVO getByKey(Long tenantId, String key);

    /**
     * 按 key 更新或新增配置
     */
    TenantConfigVO put(Long tenantId, String key, String value);

    /**
     * 列出某个租户的所有配置
     */
    List<TenantConfigVO> listByTenant(Long tenantId);
}
