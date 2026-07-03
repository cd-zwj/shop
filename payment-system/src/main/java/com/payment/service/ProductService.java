package com.payment.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.payment.dto.ProductDTO;
import com.payment.dto.ScanRequestDTO;
import com.payment.dto.ScanResponseDTO;
import com.payment.entity.Product;

import java.util.List;

/**
 * 商品服务接口。
 *
 * <p>提供商品的全生命周期管理能力，包括创建、更新、删除、查询以及扫码解析等功能。
 * 所有商品数据按多租户行级隔离（tenant_id）。</p>
 */
public interface ProductService extends IService<Product> {

    /**
     * 创建商品。
     *
     * @param dto 商品数据传输对象，包含商品名称、价格、分类、库存等信息
     * @return 创建成功后的商品实体，含自动生成的ID和商品编码
     * @throws com.payment.common.exception.BusinessException 当商品名称重复或参数校验失败时抛出
     */
    Product createProduct(ProductDTO dto);

    /**
     * 更新商品信息。
     *
     * @param id  商品ID
     * @param dto 更新后的商品数据
     * @return 更新后的商品实体
     * @throws com.payment.common.exception.BusinessException 当商品不存在时抛出
     */
    Product updateProduct(Long id, ProductDTO dto);

    /**
     * 删除商品。
     *
     * @param id 商品ID
     * @throws com.payment.common.exception.BusinessException 当商品不存在或存在未完成订单时抛出
     */
    void deleteProduct(Long id);

    /**
     * 根据商品编码查询商品。
     *
     * @param productCode 商品编码（全局唯一）
     * @return 商品实体，不存在时返回null
     */
    Product getByProductCode(String productCode);

    /**
     * 根据ID查询商品（带Redis缓存）。
     *
     * <p>优先从Redis缓存中读取，缓存未命中时查询数据库并回填缓存。</p>
     *
     * @param productId 商品ID
     * @return 商品实体，不存在时返回null
     */
    Product getProductByIdWithCache(Long productId);

    /**
     * 根据ID查询商品（不带缓存，直接查库）。
     *
     * @param productId 商品ID
     * @return 商品实体，不存在时返回null
     */
    Product getProductById(Long productId);

    /**
     * 获取商品列表（关键字 + 分类筛选）。
     *
     * @param keyword  商品名称/编码模糊搜索关键字（可空）
     * @param category 商品分类（可空）
     * @return 匹配条件的商品列表
     */
    List<Product> getProductList(String keyword, String category);

    /**
     * 分页获取商品列表。
     *
     * @param page     分页参数
     * @param category 商品分类筛选（可空）
     * @param sortBy   排序字段（可空，如 price、create_time 等）
     * @return 分页商品数据
     */
    IPage<Product> listProducts(Page<Product> page, String category, String sortBy);

    /**
     * 处理扫码请求，解析扫码内容并返回匹配的商品信息。
     *
     * @param request 扫码请求，包含扫码类型和扫码内容
     * @return 扫码响应，包含匹配到的商品详情或错误信息
     * @throws com.payment.common.exception.BusinessException 当扫码内容无法解析时抛出
     */
    ScanResponseDTO handleScan(ScanRequestDTO request);
}
