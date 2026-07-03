package com.payment.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.payment.entity.TenantConfig;
import com.payment.vo.TenantConfigVO;

import java.util.List;

/**
 * 租户配置服务接口。
 * <p>
 * 管理租户级别的 KV 配置项，支持按 key 读写和全量列出。
 * 租户配置用于存储每个租户的个性化设置，如支付策略、
 * 营销规则、功能开关等。
 */
public interface TenantConfigService extends IService<TenantConfig> {

    /**
     * 按 key 获取单个配置，不存在时创建默认值。
     *
     * @param tenantId 租户 ID
     * @param key      配置键
     * @return 租户配置视图对象
     */
    TenantConfigVO getByKey(Long tenantId, String key);

    /**
     * 按 key 更新或新增配置。
     * <p>
     * 若配置已存在则更新值，不存在则新增。
     *
     * @param tenantId 租户 ID
     * @param key      配置键
     * @param value    配置值
     * @return 更新后的租户配置视图对象
     */
    TenantConfigVO put(Long tenantId, String key, String value);

    /**
     * 列出某个租户的所有配置。
     *
     * @param tenantId 租户 ID
     * @return 租户配置视图列表
     */
    List<TenantConfigVO> listByTenant(Long tenantId);
}
