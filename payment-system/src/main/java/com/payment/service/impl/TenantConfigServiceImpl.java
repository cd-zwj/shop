package com.payment.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.payment.common.BusinessException;
import com.payment.entity.TenantConfig;
import com.payment.mapper.TenantConfigMapper;
import com.payment.service.TenantConfigService;
import com.payment.vo.TenantConfigVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 租户配置服务实现。
 * 首次查询 key 不存在时自动创建默认空值，确保前端不会拿到 null。
 */
@Slf4j
@Service
public class TenantConfigServiceImpl
        extends ServiceImpl<TenantConfigMapper, TenantConfig>
        implements TenantConfigService {

    @Override
    public TenantConfigVO getByKey(Long tenantId, String key) {
        if (tenantId == null || key == null || key.isBlank()) {
            throw new BusinessException("租户ID和配置键不能为空");
        }

        TenantConfig config = baseMapper.selectOne(
                new LambdaQueryWrapper<TenantConfig>()
                        .eq(TenantConfig::getTenantId, tenantId)
                        .eq(TenantConfig::getConfigKey, key));

        if (config == null) {
            config = createDefault(tenantId, key);
        }
        return TenantConfigVO.from(config);
    }

    @Override
    public TenantConfigVO put(Long tenantId, String key, String value) {
        if (tenantId == null || key == null || key.isBlank()) {
            throw new BusinessException("租户ID和配置键不能为空");
        }

        TenantConfig existing = baseMapper.selectOne(
                new LambdaQueryWrapper<TenantConfig>()
                        .eq(TenantConfig::getTenantId, tenantId)
                        .eq(TenantConfig::getConfigKey, key));

        if (existing == null) {
            existing = createDefault(tenantId, key);
        }
        existing.setConfigValue(value);
        existing.setUpdateTime(LocalDateTime.now());
        baseMapper.updateById(existing);
        return TenantConfigVO.from(baseMapper.selectById(existing.getId()));
    }

    @Override
    public List<TenantConfigVO> listByTenant(Long tenantId) {
        if (tenantId == null) {
            throw new BusinessException("租户ID不能为空");
        }
        return baseMapper.selectList(
                        new LambdaQueryWrapper<TenantConfig>()
                                .eq(TenantConfig::getTenantId, tenantId)
                                .orderByAsc(TenantConfig::getConfigKey))
                .stream()
                .map(TenantConfigVO::from)
                .toList();
    }

    private TenantConfig createDefault(Long tenantId, String key) {
        TenantConfig config = new TenantConfig();
        config.setTenantId(tenantId);
        config.setConfigKey(key);
        config.setConfigValue("");
        config.setConfigType("SYSTEM");
        config.setDescription("系统默认配置");
        config.setCreateTime(LocalDateTime.now());
        config.setUpdateTime(LocalDateTime.now());
        baseMapper.insert(config);
        return config;
    }
}
