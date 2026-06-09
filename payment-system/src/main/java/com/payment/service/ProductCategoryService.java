package com.payment.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.payment.entity.ProductCategory;
import com.payment.vo.ProductCategoryVO;

import java.util.List;

/**
 * 商品分类服务接口
 */
public interface ProductCategoryService extends IService<ProductCategory> {

    /**
     * 按租户查询树形分类列表
     *
     * @param tenantId 租户ID，null 表示查平台级分类
     * @return 树形分类列表
     */
    List<ProductCategoryVO> listTreeByTenant(Long tenantId);

    /**
     * 创建分类
     */
    ProductCategoryVO create(ProductCategory category);

    /**
     * 更新分类
     */
    ProductCategoryVO update(Long id, ProductCategory category);

    /**
     * 逻辑删除分类
     */
    void delete(Long id);
}
