package com.payment.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.payment.dto.V1MerchantProductUpsertDTO;
import com.payment.dto.V1MerchantProductVO;

/**
 * 商户端商品管理服务。
 *
 * <p>承接 {@code V1MerchantProductController} 的商品 CRUD、库存初始化与索引发布逻辑，
 * 所有方法在执行前校验调用方对目标商户的访问权限。</p>
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
     */
    Page<V1MerchantProductVO> listProducts(Long tenantId, Long platformUserId, Integer current, Integer size,
                                           String search, String category, String status);

    /**
     * 查询单个商户商品详情。
     */
    V1MerchantProductVO getProduct(Long tenantId, Long platformUserId, Long productId);

    /**
     * 新建商户商品并初始化库存，发布索引 upsert 消息。
     */
    V1MerchantProductVO createProduct(Long tenantId, Long platformUserId, V1MerchantProductUpsertDTO dto);

    /**
     * 更新商户商品与库存，发布索引 upsert 消息。
     */
    V1MerchantProductVO updateProduct(Long tenantId, Long platformUserId, Long productId, V1MerchantProductUpsertDTO dto);

    /**
     * 逻辑删除商户商品，发布索引 delete 消息。
     */
    void deleteProduct(Long tenantId, Long platformUserId, Long productId);
}
