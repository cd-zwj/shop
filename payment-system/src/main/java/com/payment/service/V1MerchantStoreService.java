package com.payment.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.payment.dto.V1MerchantStoreUpsertDTO;
import com.payment.dto.V1MerchantStoreVO;

/**
 * 商户端门店管理服务接口。
 *
 * <p>提供商户门店的全生命周期管理能力，包括门店的创建、查询、更新、
 * 状态变更（启用/停用）和删除。所有操作均校验调用方对目标商户的访问权限，
 * 确保多商户间数据隔离。</p>
 */
public interface V1MerchantStoreService {

    /**
     * 分页查询商户门店列表。
     *
     * @param tenantId       商户ID
     * @param platformUserId 当前登录用户ID（用于权限校验）
     * @param current        页码
     * @param size           每页条数
     * @param keyword        门店名称/地址模糊搜索（可空）
     * @param status         门店状态过滤：1-营业中 / 0-已关闭（可空）
     * @return 门店分页数据
     */
    Page<V1MerchantStoreVO> listStores(Long tenantId, Long platformUserId, Integer current, Integer size,
                                       String keyword, Integer status);

    /**
     * 查询单个门店详情。
     *
     * @param tenantId       商户ID
     * @param platformUserId 当前登录用户ID
     * @param storeId        门店ID
     * @return 门店详情视图对象
     * @throws com.payment.common.exception.BusinessException 当门店不存在或无权访问时抛出
     */
    V1MerchantStoreVO getStore(Long tenantId, Long platformUserId, Long storeId);

    /**
     * 创建门店。
     *
     * @param tenantId       商户ID
     * @param platformUserId 当前登录用户ID
     * @param dto            门店创建数据，包含名称、地址、联系方式等
     * @return 创建成功的门店视图对象
     * @throws com.payment.common.exception.BusinessException 当参数校验失败时抛出
     */
    V1MerchantStoreVO createStore(Long tenantId, Long platformUserId, V1MerchantStoreUpsertDTO dto);

    /**
     * 更新门店信息。
     *
     * @param tenantId       商户ID
     * @param platformUserId 当前登录用户ID
     * @param storeId        门店ID
     * @param dto            更新后的门店数据
     * @return 更新后的门店视图对象
     * @throws com.payment.common.exception.BusinessException 当门店不存在或无权访问时抛出
     */
    V1MerchantStoreVO updateStore(Long tenantId, Long platformUserId, Long storeId, V1MerchantStoreUpsertDTO dto);

    /**
     * 更新门店状态（启用/停用）。
     *
     * @param tenantId       商户ID
     * @param platformUserId 当前登录用户ID
     * @param storeId        门店ID
     * @param status         目标状态：1-营业中 / 0-已关闭
     * @return 更新后的门店视图对象
     */
    V1MerchantStoreVO updateStoreStatus(Long tenantId, Long platformUserId, Long storeId, Integer status);

    /**
     * 删除门店。
     *
     * <p>删除前校验门店下是否存在关联的订单或商品。</p>
     *
     * @param tenantId       商户ID
     * @param platformUserId 当前登录用户ID
     * @param storeId        门店ID
     * @throws com.payment.common.exception.BusinessException 当门店存在关联数据时抛出
     */
    void deleteStore(Long tenantId, Long platformUserId, Long storeId);
}
