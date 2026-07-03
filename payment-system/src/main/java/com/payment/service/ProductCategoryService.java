package com.payment.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.payment.entity.ProductCategory;
import com.payment.vo.ProductCategoryVO;

import java.util.List;

/**
 * 商品分类服务接口。
 *
 * <p>提供商品分类的树形结构管理能力，支持按租户隔离分类数据。
 * 分类支持父子层级关系，返回时组装为树形结构。</p>
 */
public interface ProductCategoryService extends IService<ProductCategory> {

    /**
     * 按租户查询树形分类列表。
     *
     * @param tenantId 租户ID，null 表示查平台级分类
     * @return 树形分类列表
     */
    List<ProductCategoryVO> listTreeByTenant(Long tenantId);

    /**
     * 创建商品分类。
     *
     * @param category 分类实体，包含名称、父分类ID、排序等信息
     * @return 创建成功后的分类视图对象
     * @throws com.payment.common.exception.BusinessException 当分类名称重复或父分类不存在时抛出
     */
    ProductCategoryVO create(ProductCategory category);

    /**
     * 更新商品分类。
     *
     * @param id       分类ID
     * @param category 更新后的分类数据
     * @return 更新后的分类视图对象
     * @throws com.payment.common.exception.BusinessException 当分类不存在或名称冲突时抛出
     */
    ProductCategoryVO update(Long id, ProductCategory category);

    /**
     * 逻辑删除商品分类。
     *
     * <p>删除时校验分类下是否存在关联商品，若存在则禁止删除。</p>
     *
     * @param id 分类ID
     * @throws com.payment.common.exception.BusinessException 当分类下存在商品时抛出
     */
    void delete(Long id);
}
