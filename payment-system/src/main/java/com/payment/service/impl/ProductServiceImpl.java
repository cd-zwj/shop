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
import com.payment.util.MinioUtil;
import com.payment.util.RedisUtils;
import com.payment.util.TenantContextHolder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.util.List;

/**
 * 商品服务实现类
 */
@Slf4j
@Service
public class ProductServiceImpl extends ServiceImpl<ProductMapper, Product> implements ProductService {

    private static final String PRODUCT_CACHE_PREFIX = "product:";
    private static final String PRODUCT_CODE_CACHE_PREFIX = "product:code:";
    private static final long PRODUCT_CACHE_EXPIRE_MINUTES = 30L;
    private static final long PRODUCT_CACHE_JITTER_SECONDS = 300L;

    @Autowired
    private ProductStockMapper productStockMapper;

    @Autowired
    private ScanRecordMapper scanRecordMapper;

    @Autowired
    private MinioUtil minioUtil;

    @Autowired
    private RedisUtils redisUtils;

    @Autowired(required = false)
    private com.payment.service.ProductSearchService productSearchService;

    private String generateProductCacheKey(Long tenantId, Long productId) {
        return PRODUCT_CACHE_PREFIX + tenantId + ":" + productId;
    }

    private String generateProductCodeCacheKey(Long tenantId, String productCode) {
        return PRODUCT_CODE_CACHE_PREFIX + tenantId + ":" + productCode;
    }

    private void setProductToCache(Product product) {
        try {
            redisUtils.setJsonWithRandomTtl(
                    generateProductCacheKey(product.getTenantId(), product.getId()),
                    product,
                    Duration.ofMinutes(PRODUCT_CACHE_EXPIRE_MINUTES),
                    PRODUCT_CACHE_JITTER_SECONDS
            );
            redisUtils.setJsonWithRandomTtl(
                    generateProductCodeCacheKey(product.getTenantId(), product.getProductCode()),
                    product,
                    Duration.ofMinutes(PRODUCT_CACHE_EXPIRE_MINUTES),
                    PRODUCT_CACHE_JITTER_SECONDS
            );
            log.info("商品写入 Redis 缓存，tenantId={}, productId={}", product.getTenantId(), product.getId());
        } catch (Exception e) {
            log.error("商品写入 Redis 缓存失败", e);
        }
    }

    private void deleteProductCache(Long tenantId, Long productId) {
        try {
            Product product = getById(productId);
            redisUtils.delete(generateProductCacheKey(tenantId, productId));
            if (product != null && product.getProductCode() != null) {
                redisUtils.delete(generateProductCodeCacheKey(tenantId, product.getProductCode()));
            }
            log.info("删除商品 Redis 缓存，tenantId={}, productId={}", tenantId, productId);
        } catch (Exception e) {
            log.error("删除商品 Redis 缓存失败", e);
        }
    }

    public Product getProductByIdWithCache(Long productId) {
        Long tenantId = TenantContextHolder.getTenantId();
        if (tenantId == null) {
            throw new BusinessException("租户信息不存在");
        }

        return redisUtils.queryWithMutex(
                generateProductCacheKey(tenantId, productId),
                Product.class,
                Duration.ofMinutes(PRODUCT_CACHE_EXPIRE_MINUTES),
                Duration.ofMinutes(2),
                PRODUCT_CACHE_JITTER_SECONDS,
                () -> {
                    Product product = getById(productId);
                    if (product == null || product.getDeleted() == 1 || !tenantId.equals(product.getTenantId())) {
                        return null;
                    }
                    return product;
                }
        );
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Product createProduct(ProductDTO dto) {
        Long tenantId = TenantContextHolder.getTenantId();
        if (tenantId == null) {
            throw new BusinessException("租户信息不存在");
        }

        Product existProduct = getByProductCode(dto.getProductCode());
        if (existProduct != null) {
            throw new BusinessException("商品编码已存在");
        }

        Product product = new Product();
        BeanUtils.copyProperties(dto, product);
        product.setTenantId(tenantId);

        if (dto.getImage() != null && !dto.getImage().isEmpty()) {
            String imageUrl = minioUtil.uploadFile(dto.getImage());
            product.setImageUrl(imageUrl);
        }

        save(product);

        ProductStock stock = new ProductStock();
        stock.setTenantId(tenantId);
        stock.setProductId(product.getId());
        stock.setQuantity(0);
        productStockMapper.insert(stock);

        setProductToCache(product);

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

        if (!product.getProductCode().equals(dto.getProductCode())) {
            Product existProduct = getByProductCode(dto.getProductCode());
            if (existProduct != null && !existProduct.getId().equals(id)) {
                throw new BusinessException("商品编码已被使用");
            }
        }

        String oldProductCode = product.getProductCode();
        BeanUtils.copyProperties(dto, product, "id", "tenantId", "imageUrl");

        if (dto.getImage() != null && !dto.getImage().isEmpty()) {
            if (product.getImageUrl() != null) {
                String objectName = minioUtil.extractObjectNameFromUrl(product.getImageUrl());
                minioUtil.deleteFile(objectName);
            }
            String imageUrl = minioUtil.uploadFile(dto.getImage());
            product.setImageUrl(imageUrl);
        }

        updateById(product);

        redisUtils.delete(generateProductCacheKey(product.getTenantId(), product.getId()));
        redisUtils.delete(generateProductCodeCacheKey(product.getTenantId(), oldProductCode));
        setProductToCache(product);

        if (productSearchService != null) {
            productSearchService.syncProduct(product);
        }

        return product;
    }

    @Override
    public Product getByProductCode(String productCode) {
        Long tenantId = TenantContextHolder.getTenantId();
        if (tenantId == null) {
            throw new BusinessException("租户信息不存在");
        }

        Product product = redisUtils.queryWithMutex(
                generateProductCodeCacheKey(tenantId, productCode),
                Product.class,
                Duration.ofMinutes(PRODUCT_CACHE_EXPIRE_MINUTES),
                Duration.ofMinutes(2),
                PRODUCT_CACHE_JITTER_SECONDS,
                () -> getOne(new LambdaQueryWrapper<Product>()
                        .eq(Product::getTenantId, tenantId)
                        .eq(Product::getProductCode, productCode)
                        .eq(Product::getDeleted, 0))
        );

        if (product != null) {
            redisUtils.setJsonWithRandomTtl(
                    generateProductCacheKey(product.getTenantId(), product.getId()),
                    product,
                    Duration.ofMinutes(PRODUCT_CACHE_EXPIRE_MINUTES),
                    PRODUCT_CACHE_JITTER_SECONDS
            );
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
                .eq(Product::getStatus, 1);

        if (tenantId != null) {
            wrapper.eq(Product::getTenantId, tenantId);
        }

        if (category != null && !category.isEmpty()) {
            wrapper.eq(Product::getCategory, category);
        }

        if ("price_asc".equals(sortBy)) {
            wrapper.orderByAsc(Product::getPrice);
        } else if ("price_desc".equals(sortBy)) {
            wrapper.orderByDesc(Product::getPrice);
        } else {
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
            Product product = getByProductCode(request.getProductCode());

            if (product == null) {
                response.setStatus("NOT_FOUND");
                response.setMessage("商品不存在");
                saveScanRecord(request, null, "NOT_FOUND", "商品不存在");
                return response;
            }

            if (product.getStatus() == 0) {
                response.setStatus("ERROR");
                response.setMessage("商品已下架");
                saveScanRecord(request, product.getId(), "ERROR", "商品已下架");
                return response;
            }

            ProductStock stock = productStockMapper.selectOne(
                    new LambdaQueryWrapper<ProductStock>()
                            .eq(ProductStock::getProductId, product.getId())
            );

            Integer stockQuantity = stock != null ? stock.getQuantity() : 0;
            Integer quantity = request.getQuantity() != null ? request.getQuantity() : 1;
            if (stockQuantity < quantity) {
                response.setStatus("OUT_OF_STOCK");
                response.setMessage("库存不足");
                saveScanRecord(request, product.getId(), "OUT_OF_STOCK", "库存不足");
                return response;
            }

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
