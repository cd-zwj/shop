package com.payment.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.payment.dto.MerchantDTO;
import com.payment.dto.MerchantDetailVO;
import com.payment.dto.MerchantQueryDTO;
import com.payment.entity.Tenant;

/**
 * 商家（商户）管理服务接口。
 *
 * <p>提供平台级商户管理能力，包括商户的创建、信息更新、启用/禁用、
 * 列表查询、详情查看以及平台数据概览统计等功能。
 * 商户本质上对应系统中的租户（Tenant）实体。</p>
 */
public interface MerchantService extends IService<Tenant> {

    /**
     * 创建商家（租户）。
     *
     * <p>注册新商户并初始化租户基础数据（如默认角色、权限等）。</p>
     *
     * @param dto 商户信息DTO，包含名称、联系方式、经营类目等
     * @return 创建成功的租户实体
     * @throws com.payment.common.exception.BusinessException 当商户名称重复或参数校验失败时抛出
     */
    Tenant createMerchant(MerchantDTO dto);

    /**
     * 更新商家信息。
     *
     * @param tenantId 租户ID
     * @param dto      更新后的商户信息
     * @throws com.payment.common.exception.BusinessException 当商户不存在时抛出
     */
    void updateMerchant(Long tenantId, MerchantDTO dto);

    /**
     * 启用商家（恢复商户正常运营状态）。
     *
     * @param tenantId 租户ID
     */
    void enableMerchant(Long tenantId);

    /**
     * 禁用商家（暂停商户运营，禁止新交易）。
     *
     * @param tenantId 租户ID
     */
    void disableMerchant(Long tenantId);

    /**
     * 分页查询商家列表。
     *
     * @param query 查询条件，包含名称模糊搜索、状态过滤、分页参数等
     * @return 商家分页数据
     */
    Page<Tenant> listMerchants(MerchantQueryDTO query);

    /**
     * 查询商家详情（含统计数据）。
     *
     * @param tenantId 租户ID
     * @return 商家详情视图对象，包含基本信息和经营数据
     * @throws com.payment.common.exception.BusinessException 当商户不存在时抛出
     */
    MerchantDetailVO getMerchantDetail(Long tenantId);

    /**
     * 获取平台数据概览（商户总数、今日交易额、活跃用户数等）。
     *
     * @return 平台概览数据键值对
     */
    java.util.Map<String, Object> getDashboardStats();

    /**
     * 获取商家注册趋势数据（按时间维度统计新增商户数）。
     *
     * @return 趋势数据，包含时间和数量
     */
    java.util.Map<String, Object> getMerchantTrend();

    /**
     * 获取平台销售趋势数据（按时间维度统计交易额）。
     *
     * @return 趋势数据，包含时间和金额
     */
    java.util.Map<String, Object> getSalesTrend();
}
