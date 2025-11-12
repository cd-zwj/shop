package com.payment.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.payment.common.BusinessException;
import com.payment.dto.ProductDTO;
import com.payment.dto.ScanRequestDTO;
import com.payment.dto.ScanResponseDTO;
import com.payment.entity.Product;
import com.payment.entity.ProductStock;
import com.payment.entity.ScanRecord;
import com.payment.mapper.ProductMapper;
import com.payment.mapper.ProductStockMapper;
import com.payment.mapper.ScanRecordMapper;
import com.payment.service.ProductService;
import com.payment.util.OssUtil;
import com.payment.util.TenantContextHolder;
import com.alibaba.fastjson2.JSON;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * 商品服务实现类
 */
@Slf4j
@Service
public class ProductServiceImpl extends ServiceImpl<ProductMapper, Product> implements ProductService {
    
    private static final String PRODUCT_CACHE_PREFIX = "product:";
    private static final long PRODUCT_CACHE_EXPIRE = 30; // 30分钟
    
    @Autowired
    private ProductStockMapper productStockMapper;
    
    @Autowired
    private ScanRecordMapper scanRecordMapper;
    
    @Autowired
    private OssUtil ossUtil;
    
    @Autowired
    private StringRedisTemplate stringRedisTemplate;
    
    @Autowired(required = false)
    private com.payment.service.ProductSearchService productSearchService;
    
    /**
     * 生成商品缓存Key
     */
    private String generateProductCacheKey(Long tenantId, Long productId) {
        return PRODUCT_CACHE_PREFIX + tenantId + ":" + productId;
    }
    
    /**
     * 从Redis获取商品缓存
     */
    private Product getProductFromCache(Long tenantId, Long productId) {
        try {
            String key = generateProductCacheKey(tenantId, productId);
            String json = stringRedisTemplate.opsForValue().get(key);
            if (json != null) {
                log.info("从Redis缓存获取商品，tenantId={}, productId={}", tenantId, productId);
                return JSON.parseObject(json, Product.class);
            }
        } catch (Exception e) {
            log.error("从Redis获取商品缓存失败", e);
        }
        return null;
    }
    
    /**
     * 将商品写入Redis缓存
     */
    private void setProductToCache(Product product) {
        try {
            String key = generateProductCacheKey(product.getTenantId(), product.getId());
            String json = JSON.toJSONString(product);
            stringRedisTemplate.opsForValue().set(key, json, PRODUCT_CACHE_EXPIRE, TimeUnit.MINUTES);
            log.info("商品写入Redis缓存，tenantId={}, productId={}", product.getTenantId(), product.getId());
        } catch (Exception e) {
            log.error("商品写入Redis缓存失败", e);
        }
    }
    
    /**
     * 删除商品缓存
     */
    private void deleteProductCache(Long tenantId, Long productId) {
        try {
            String key = generateProductCacheKey(tenantId, productId);
            stringRedisTemplate.delete(key);
            log.info("删除商品Redis缓存，tenantId={}, productId={}", tenantId, productId);
        } catch (Exception e) {
            log.error("删除商品Redis缓存失败", e);
        }
    }
    
    /**
     * 根据ID查询商品（带缓存）
     */
    public Product getProductByIdWithCache(Long productId) {
        Long tenantId = TenantContextHolder.getTenantId();
        if (tenantId == null) {
            throw new BusinessException("租户信息不存在");
        }
        
        // 先从缓存查询
        Product product = getProductFromCache(tenantId, productId);
        if (product != null) {
            return product;
        }
        
        // 缓存未命中，从数据库查询
        product = getById(productId);
        if (product != null && product.getDeleted() == 0) {
            // 写入缓存
            setProductToCache(product);
            return product;
        }
        
        return null;
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Product createProduct(ProductDTO dto) {
        Long tenantId = TenantContextHolder.getTenantId();
        if (tenantId == null) {
            throw new BusinessException("租户信息不存在");
        }
        
        // 检查商品编码是否已存在
        Product existProduct = getByProductCode(dto.getProductCode());
        if (existProduct != null) {
            throw new BusinessException("商品编码已存在");
        }
        
        // 创建商品
        Product product = new Product();
        BeanUtils.copyProperties(dto, product);
        product.setTenantId(tenantId);
        
        // 上传图片
        if (dto.getImage() != null && !dto.getImage().isEmpty()) {
            String imageUrl = ossUtil.uploadFile(dto.getImage());
            product.setImageUrl(imageUrl);
        }
        
        save(product);
        
        // 初始化库存
        ProductStock stock = new ProductStock();
        stock.setTenantId(tenantId);
        stock.setProductId(product.getId());
        stock.setQuantity(0);
        productStockMapper.insert(stock);
        
        // 写入缓存
        setProductToCache(product);
        
        // 同步到Elasticsearch
        if (productSearchService != null) {
            productSearchService.syncProduct(product);
        }
        
        return product;
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Product updateProduct(Long id, ProductDTO dto) {
        Product product = getById(id);
        if (product == null) {
            throw new BusinessException("商品不存在");
        }
        
        // 检查商品编码是否被其他商品使用
        if (!product.getProductCode().equals(dto.getProductCode())) {
            Product existProduct = getByProductCode(dto.getProductCode());
            if (existProduct != null && !existProduct.getId().equals(id)) {
                throw new BusinessException("商品编码已被使用");
            }
        }
        
        // 更新商品信息
        BeanUtils.copyProperties(dto, product, "id", "tenantId", "imageUrl");
        
        // 更新图片
        if (dto.getImage() != null && !dto.getImage().isEmpty()) {
            // 删除旧图片
            if (product.getImageUrl() != null) {
                ossUtil.deleteFile(product.getImageUrl());
            }
            // 上传新图片
            String imageUrl = ossUtil.uploadFile(dto.getImage());
            product.setImageUrl(imageUrl);
        }
        
        updateById(product);
        
        // 更新缓存
        deleteProductCache(product.getTenantId(), product.getId());
        setProductToCache(product);
        
        // 同步到Elasticsearch
        if (productSearchService != null) {
            productSearchService.syncProduct(product);
        }
        
        return product;
    }
    
    @Override
    public Product getByProductCode(String productCode) {
        Long tenantId = TenantContextHolder.getTenantId();
        
        // 从数据库查询商品
        Product product = getOne(new LambdaQueryWrapper<Product>()
                .eq(Product::getTenantId, tenantId)
                .eq(Product::getProductCode, productCode)
                .eq(Product::getDeleted, 0));
        
        // 如果商品存在，写入缓存
        if (product != null) {
            setProductToCache(product);
        }
        
        return product;
    }
    
    @Override
    public Product getProductById(Long productId) {
        return getProductByIdWithCache(productId);
    }
    
    @Override
    public List<Product> getProductList(String keyword, String category) {
        Long tenantId = TenantContextHolder.getTenantId();
        LambdaQueryWrapper<Product> wrapper = new LambdaQueryWrapper<Product>()
                .eq(Product::getTenantId, tenantId)
                .eq(Product::getDeleted, 0)
                .orderByDesc(Product::getCreateTime);
        
        if (keyword != null && !keyword.isEmpty()) {
            wrapper.and(w -> w.like(Product::getName, keyword)
                    .or()
                    .like(Product::getProductCode, keyword));
        }
        
        if (category != null && !category.isEmpty()) {
            wrapper.eq(Product::getCategory, category);
        }
        
        return list(wrapper);
    }
    
    @Override
    public com.baomidou.mybatisplus.core.metadata.IPage<Product> listProducts(
            com.baomidou.mybatisplus.extension.plugins.pagination.Page<Product> page, 
            String category, 
            String sortBy) {
        Long tenantId = TenantContextHolder.getTenantId();
        
        LambdaQueryWrapper<Product> wrapper = new LambdaQueryWrapper<Product>()
                .eq(Product::getDeleted, 0)
                .eq(Product::getStatus, 1); // 只显示上架商品
        
        // 如果有租户ID，则按租户过滤
        if (tenantId != null) {
            wrapper.eq(Product::getTenantId, tenantId);
        }
        
        // 分类过滤
        if (category != null && !category.isEmpty()) {
            wrapper.eq(Product::getCategory, category);
        }
        
        // 排序
        if ("price_asc".equals(sortBy)) {
            wrapper.orderByAsc(Product::getPrice);
        } else if ("price_desc".equals(sortBy)) {
            wrapper.orderByDesc(Product::getPrice);
        } else if ("sales".equals(sortBy)) {
            // 按销量排序（假设有销量字段）
            wrapper.orderByDesc(Product::getCreateTime);
        } else {
            // 默认按创建时间倒序
            wrapper.orderByDesc(Product::getCreateTime);
        }
        
        return page(page, wrapper);
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public ScanResponseDTO handleScan(ScanRequestDTO request) {
        ScanResponseDTO response = new ScanResponseDTO();
        response.setProductCode(request.getProductCode());
        
        try {
            // 设置租户上下文（从请求中获取）
            // 这里需要根据tenantCode查询tenantId并设置
            // 简化处理，假设已经设置
            
            // 查询商品
            Product product = getByProductCode(request.getProductCode());
            
            if (product == null) {
                response.setStatus("NOT_FOUND");
                response.setMessage("商品不存在");
                saveScanRecord(request, null, "NOT_FOUND", "商品不存在");
                return response;
            }
            
            // 检查商品状态
            if (product.getStatus() == 0) {
                response.setStatus("ERROR");
                response.setMessage("商品已下架");
                saveScanRecord(request, product.getId(), "ERROR", "商品已下架");
                return response;
            }
            
            // 查询库存
            ProductStock stock = productStockMapper.selectOne(
                    new LambdaQueryWrapper<ProductStock>()
                            .eq(ProductStock::getProductId, product.getId())
            );
            
            Integer stockQuantity = stock != null ? stock.getQuantity() : 0;
            
            // 检查库存
            Integer quantity = request.getQuantity() != null ? request.getQuantity() : 1;
            if (stockQuantity < quantity) {
                response.setStatus("OUT_OF_STOCK");
                response.setMessage("库存不足");
                saveScanRecord(request, product.getId(), "OUT_OF_STOCK", "库存不足");
                return response;
            }
            
            // 返回商品信息
            response.setStatus("SUCCESS");
            response.setProductId(product.getId());
            response.setProductName(product.getName());
            response.setProductImage(product.getImageUrl());
            response.setPrice(product.getPrice());
            response.setStock(stockQuantity);
            response.setMessage("扫码成功");
            
            saveScanRecord(request, product.getId(), "SUCCESS", null);
            
        } catch (Exception e) {
            log.error("处理扫码请求失败", e);
            response.setStatus("ERROR");
            response.setMessage("处理失败：" + e.getMessage());
            saveScanRecord(request, null, "ERROR", e.getMessage());
        }
        
        return response;
    }
    
    /**
     * 保存扫码记录
     */
    private void saveScanRecord(ScanRequestDTO request, Long productId, String status, String errorMessage) {
        try {
            ScanRecord record = new ScanRecord();
            record.setTenantId(TenantContextHolder.getTenantId());
            record.setDeviceId(request.getDeviceId());
            record.setProductCode(request.getProductCode());
            record.setProductId(productId);
            record.setScanStatus(status);
            record.setErrorMessage(errorMessage);
            scanRecordMapper.insert(record);
        } catch (Exception e) {
            log.error("保存扫码记录失败", e);
        }
    }
}

