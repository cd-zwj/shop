package com.payment.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.payment.dto.V1MerchantProductUpsertDTO;
import com.payment.dto.V1MerchantProductVO;

/**
 * 商户端商品管理服务接口。
 *
 * <p>承接 {@code V1MerchantProductController} 的商品 CRUD、库存初始化与索引发布逻辑。
 * 所有方法在执行前校验调用方对目标商户的访问权限，确保商户间数据隔离。</p>
 */
public interface V1MerchantProductService {

    /**
     * 分页查询商户商品。
     *
     * @param tenantId       商户ID
     * @param platformUserId 当前登录用户ID（用于商户权限校验）
     * @param current        页码
     * @param size           每页条数
     * @param search         商品名称/编码模糊搜索（可空）
     * @param category       商品分类（可空）
     * @param status         商品状态过滤：active / inactive / out_of_stock（可空）
     * @return 商品分页数据
     */
    Page<V1MerchantProductVO> listProducts(Long tenantId, Long platformUserId, Integer current, Integer size,
                                           String search, String category, String status);

    /**
     * 查询单个商户商品详情。
     *
     * @param tenantId       商户ID
     * @param platformUserId 当前登录用户ID
     * @param productId      商品ID
     * @return 商品详情视图对象
     * @throws com.payment.common.exception.BusinessException 当商品不存在或无权访问时抛出
     */
    V1MerchantProductVO getProduct(Long tenantId, Long platformUserId, Long productId);

    /**
     * 新建商户商品并初始化库存，同时发布ES索引 upsert 消息。
     *
     * @param tenantId       商户ID
     * @param platformUserId 当前登录用户ID
     * @param dto            商品创建/更新DTO
     * @return 创建成功的商品视图对象
     * @throws com.payment.common.exception.BusinessException 当商品名称重复或参数校验失败时抛出
     */
    V1MerchantProductVO createProduct(Long tenantId, Long platformUserId, V1MerchantProductUpsertDTO dto);

    /**
     * 更新商户商品与库存，同时发布ES索引 upsert 消息。
     *
     * @param tenantId       商户ID
     * @param platformUserId 当前登录用户ID
     * @param productId      商品ID
     * @param dto            更新后的商品数据
     * @return 更新后的商品视图对象
     * @throws com.payment.common.exception.BusinessException 当商品不存在或无权访问时抛出
     */
    V1MerchantProductVO updateProduct(Long tenantId, Long platformUserId, Long productId, V1MerchantProductUpsertDTO dto);

    /**
     * 逻辑删除商户商品，同时发布ES索引 delete 消息。
     *
     * @param tenantId       商户ID
     * @param platformUserId 当前登录用户ID
     * @param productId      商品ID
     * @throws com.payment.common.exception.BusinessException 当商品不存在或存在未完成订单时抛出
     */
    void deleteProduct(Long tenantId, Long platformUserId, Long productId);
}
