package com.payment.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.payment.dto.PlatformAuthProviderDTO;
import com.payment.dto.PlatformAuthProviderVO;

/**
 * 第三方登录方式管理服务接口，用于定义第三方登录方式管理相关业务能力。
 */
public interface PlatformAuthProviderAdminService {

    /**
     * 查询渠道。
     */
    Page<PlatformAuthProviderVO> listProviders(Integer current, Integer size, String keyword, Integer status);

    /**
     * 获取渠道。
     */
    PlatformAuthProviderVO getProvider(Long providerId);

    /**
     * 创建渠道。
     */
    PlatformAuthProviderVO createProvider(PlatformAuthProviderDTO dto);

    /**
     * 更新渠道。
     */
    void updateProvider(Long providerId, PlatformAuthProviderDTO dto);

    /**
     * 处理enable渠道。
     */
    void enableProvider(Long providerId);

    /**
     * 处理disable渠道。
     */
    void disableProvider(Long providerId);
}
