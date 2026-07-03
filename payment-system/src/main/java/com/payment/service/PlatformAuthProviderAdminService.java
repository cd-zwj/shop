package com.payment.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.payment.dto.PlatformAuthProviderDTO;
import com.payment.dto.PlatformAuthProviderVO;

/**
 * 第三方登录方式管理服务接口。
 *
 * <p>面向平台管理员提供第三方登录渠道（如微信、支付宝、GitHub 等）的增删改查和启停管理，
 * 承接 {@code V1AdminAuthProviderController} 的业务逻辑。</p>
 */
public interface PlatformAuthProviderAdminService {

    /**
     * 分页查询第三方登录渠道列表。
     *
     * @param current 当前页码
     * @param size    每页条数
     * @param keyword 关键字筛选（可为null，匹配渠道名称等）
     * @param status  状态筛选（可为null，1-启用 / 0-停用）
     * @return 渠道分页结果
     */
    Page<PlatformAuthProviderVO> listProviders(Integer current, Integer size, String keyword, Integer status);

    /**
     * 获取单个第三方登录渠道详情。
     *
     * @param providerId 渠道ID
     * @return 渠道详情 VO
     * @throws com.payment.common.exception.BusinessException 渠道不存在时抛出
     */
    PlatformAuthProviderVO getProvider(Long providerId);

    /**
     * 创建第三方登录渠道。
     *
     * @param dto 创建请求 DTO（含渠道名称、类型、AppId、回调地址等）
     * @return 创建成功的渠道信息
     * @throws com.payment.common.exception.BusinessException 渠道名称重复时抛出
     */
    PlatformAuthProviderVO createProvider(PlatformAuthProviderDTO dto);

    /**
     * 更新第三方登录渠道配置。
     *
     * @param providerId 渠道ID
     * @param dto        更新请求 DTO
     * @throws com.payment.common.exception.BusinessException 渠道不存在时抛出
     */
    void updateProvider(Long providerId, PlatformAuthProviderDTO dto);

    /**
     * 启用第三方登录渠道。
     *
     * @param providerId 渠道ID
     * @throws com.payment.common.exception.BusinessException 渠道不存在时抛出
     */
    void enableProvider(Long providerId);

    /**
     * 停用第三方登录渠道。
     *
     * @param providerId 渠道ID
     * @throws com.payment.common.exception.BusinessException 渠道不存在时抛出
     */
    void disableProvider(Long providerId);
}
