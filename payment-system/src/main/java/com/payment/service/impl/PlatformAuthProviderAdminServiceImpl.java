package com.payment.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.payment.common.BusinessException;
import com.payment.dto.PlatformAuthProviderDTO;
import com.payment.dto.PlatformAuthProviderVO;
import com.payment.entity.PlatformAuthProvider;
import com.payment.mapper.PlatformAuthProviderMapper;
import com.payment.service.PlatformAuthProviderAdminService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.Locale;
import java.util.stream.Collectors;

/**
 * 第三方登录方式管理服务实现类，用于实现第三方登录方式管理相关业务逻辑。
 */
@Service
@RequiredArgsConstructor
public class PlatformAuthProviderAdminServiceImpl implements PlatformAuthProviderAdminService {

    private final PlatformAuthProviderMapper platformAuthProviderMapper;

    /**
     * 查询渠道。
     */
    @Override
    public Page<PlatformAuthProviderVO> listProviders(Integer current, Integer size, String keyword, Integer status) {
        Page<PlatformAuthProvider> page = new Page<>(current, size);
        LambdaQueryWrapper<PlatformAuthProvider> wrapper = new LambdaQueryWrapper<PlatformAuthProvider>()
                .eq(status != null, PlatformAuthProvider::getStatus, status)
                .and(StringUtils.hasText(keyword), q -> q.like(PlatformAuthProvider::getProviderCode, keyword)
                        .or()
                        .like(PlatformAuthProvider::getProviderName, keyword))
                .orderByAsc(PlatformAuthProvider::getSortOrder)
                .orderByDesc(PlatformAuthProvider::getCreateTime);
        Page<PlatformAuthProvider> providerPage = platformAuthProviderMapper.selectPage(page, wrapper);
        Page<PlatformAuthProviderVO> result = new Page<>(providerPage.getCurrent(), providerPage.getSize(), providerPage.getTotal());
        result.setRecords(providerPage.getRecords().stream().map(this::toVO).collect(Collectors.toList()));
        return result;
    }

    /**
     * 获取渠道。
     */
    @Override
    public PlatformAuthProviderVO getProvider(Long providerId) {
        return toVO(requireProvider(providerId));
    }

    /**
     * 创建渠道。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public PlatformAuthProviderVO createProvider(PlatformAuthProviderDTO dto) {
        String normalizedCode = normalizeProviderCode(dto.getProviderCode());
        ensureProviderCodeAvailable(null, normalizedCode);

        PlatformAuthProvider provider = new PlatformAuthProvider();
        applyChanges(provider, dto, normalizedCode);
        if (provider.getStatus() == null) {
            provider.setStatus(1);
        }
        if (provider.getSortOrder() == null) {
            provider.setSortOrder(0);
        }
        platformAuthProviderMapper.insert(provider);
        return toVO(provider);
    }

    /**
     * 更新渠道。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateProvider(Long providerId, PlatformAuthProviderDTO dto) {
        PlatformAuthProvider provider = requireProvider(providerId);
        String normalizedCode = normalizeProviderCode(dto.getProviderCode());
        ensureProviderCodeAvailable(providerId, normalizedCode);
        applyChanges(provider, dto, normalizedCode);
        platformAuthProviderMapper.updateById(provider);
    }

    /**
     * 处理enable渠道。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void enableProvider(Long providerId) {
        updateProviderStatus(providerId, 1);
    }

    /**
     * 处理disable渠道。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void disableProvider(Long providerId) {
        updateProviderStatus(providerId, 0);
    }

    /**
     * 处理applyChange。
     */
    private void applyChanges(PlatformAuthProvider provider, PlatformAuthProviderDTO dto, String normalizedCode) {
        provider.setProviderCode(normalizedCode);
        provider.setProviderName(dto.getProviderName().trim());
        provider.setStatus(dto.getStatus());
        provider.setSortOrder(dto.getSortOrder());
        provider.setAppId(trimToNull(dto.getAppId()));
        provider.setClientId(trimToNull(dto.getClientId()));
        provider.setRedirectUri(trimToNull(dto.getRedirectUri()));
        provider.setExtJson(trimToNull(dto.getExtJson()));
    }

    /**
     * 处理ensure渠道编码Available。
     */
    private void ensureProviderCodeAvailable(Long providerId, String providerCode) {
        PlatformAuthProvider existing = platformAuthProviderMapper.selectOne(new LambdaQueryWrapper<PlatformAuthProvider>()
                .eq(PlatformAuthProvider::getProviderCode, providerCode)
                .last("limit 1"));
        if (existing != null && !existing.getId().equals(providerId)) {
            throw new BusinessException("第三方登录方式编码已存在");
        }
    }

    /**
     * 处理require渠道。
     */
    private PlatformAuthProvider requireProvider(Long providerId) {
        PlatformAuthProvider provider = platformAuthProviderMapper.selectById(providerId);
        if (provider == null) {
            throw new BusinessException("第三方登录方式不存在");
        }
        return provider;
    }

    /**
     * 更新渠道状态。
     */
    private void updateProviderStatus(Long providerId, Integer status) {
        PlatformAuthProvider provider = requireProvider(providerId);
        provider.setStatus(status);
        platformAuthProviderMapper.updateById(provider);
    }

    /**
     * 转换为VO。
     */
    private PlatformAuthProviderVO toVO(PlatformAuthProvider provider) {
        PlatformAuthProviderVO vo = new PlatformAuthProviderVO();
        vo.setId(provider.getId());
        vo.setProviderCode(provider.getProviderCode());
        vo.setProviderName(provider.getProviderName());
        vo.setStatus(provider.getStatus());
        vo.setSortOrder(provider.getSortOrder());
        vo.setAppId(provider.getAppId());
        vo.setClientId(provider.getClientId());
        vo.setRedirectUri(provider.getRedirectUri());
        vo.setExtJson(provider.getExtJson());
        vo.setCreateTime(provider.getCreateTime());
        vo.setUpdateTime(provider.getUpdateTime());
        return vo;
    }

    /**
     * 规范化渠道编码。
     */
    private String normalizeProviderCode(String providerCode) {
        if (!StringUtils.hasText(providerCode)) {
            return null;
        }
        return providerCode.trim().toUpperCase(Locale.ROOT);
    }

    /**
     * 裁剪ToNull。
     */
    private String trimToNull(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.trim();
    }
}
