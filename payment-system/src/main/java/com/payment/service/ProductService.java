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
 * 商品服务接口
 */
public interface ProductService extends IService<Product> {
    
    /**
     * 创建商品
     */
    Product createProduct(ProductDTO dto);
    
    /**
     * 更新商品
     */
    Product updateProduct(Long id, ProductDTO dto);
    
    /**
     * 根据商品编码查询商品
     */
    Product getByProductCode(String productCode);
    
    /**
     * 根据ID查询商品（带缓存）
     */
    Product getProductByIdWithCache(Long productId);
    
    /**
     * 根据ID查询商品
     */
    Product getProductById(Long productId);
    
    /**
     * 获取商品列表
     */
    List<Product> getProductList(String keyword, String category);
    
    /**
     * 获取商品列表（分页）
     */
    IPage<Product> listProducts(Page<Product> page, String category, String sortBy);
    
    /**
     * 处理扫码请求
     */
    ScanResponseDTO handleScan(ScanRequestDTO request);
}

