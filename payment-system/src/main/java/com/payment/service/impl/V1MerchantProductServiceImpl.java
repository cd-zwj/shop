package com.payment.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.payment.common.BusinessException;
import com.payment.dto.V1MerchantProductUpsertDTO;
import com.payment.dto.V1MerchantProductVO;
import com.payment.entity.Product;
import com.payment.entity.ProductStock;
import com.payment.mapper.ProductMapper;
import com.payment.mapper.ProductStockMapper;
import com.payment.service.V1MerchantProductService;
import com.payment.util.BizNoGenerator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 商户端商品管理服务实现。
 */
@Service
@RequiredArgsConstructor
public class V1MerchantProductServiceImpl implements V1MerchantProductService {

    private final ProductMapper productMapper;
    private final ProductStockMapper productStockMapper;
    private final V1MerchantSupportService v1MerchantSupportService;
    private final ProductIndexMessagePublisher productIndexMessagePublisher;

    @Override
    public Page<V1MerchantProductVO> listProducts(Long tenantId, Long platformUserId, Integer current, Integer size,
                                                  String search, String category, String status) {
        v1MerchantSupportService.requireEmployee(tenantId, platformUserId);

        boolean filterOutOfStock = "out_of_stock".equalsIgnoreCase(status);
        // 缺货过滤需要先按库存表锁定 productId 子集，再让 product 分页查询使用，
        // 保证 total 计入"缺货商品总数"而非"全部商品总数"，跨页也不会漏。
        List<Long> outOfStockProductIds = filterOutOfStock ? loadOutOfStockProductIds(tenantId) : List.of();
        if (filterOutOfStock && outOfStockProductIds.isEmpty()) {
            // 该租户无缺货商品时直接返回空页，避免向 Mapper 传空集合触发 in() 全表扫描
            return new Page<>(current, size, 0L);
        }

        Page<Product> entityPage = new Page<>(current, size);
        LambdaQueryWrapper<Product> wrapper = new LambdaQueryWrapper<Product>()
                .eq(Product::getTenantId, tenantId)
                .eq(Product::getDeleted, 0)
                .eq("active".equalsIgnoreCase(status) || "inactive".equalsIgnoreCase(status), Product::getStatus, "active".equalsIgnoreCase(status) ? 1 : 0)
                .eq(category != null && !category.isBlank(), Product::getCategory, category)
                .and(search != null && !search.isBlank(), q -> q.like(Product::getName, search).or().like(Product::getProductCode, search))
                .in(filterOutOfStock, Product::getId, outOfStockProductIds)
                .orderByDesc(Product::getCreateTime);

        Page<Product> page = productMapper.selectPage(entityPage, wrapper);
        Map<Long, ProductStock> stockMap = loadStockMap(tenantId, page.getRecords());

        List<V1MerchantProductVO> records = page.getRecords().stream()
                .map(product -> toProductVO(product, stockMap.get(product.getId())))
                .toList();

        Page<V1MerchantProductVO> result = new Page<>(page.getCurrent(), page.getSize(), page.getTotal());
        result.setRecords(records);
        return result;
    }

    @Override
    public V1MerchantProductVO getProduct(Long tenantId, Long platformUserId, Long productId) {
        v1MerchantSupportService.requireEmployee(tenantId, platformUserId);
        Product product = getTenantProduct(tenantId, productId);
        ProductStock stock = getOrCreateStock(tenantId, productId);
        return toProductVO(product, stock);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public V1MerchantProductVO createProduct(Long tenantId, Long platformUserId, V1MerchantProductUpsertDTO dto) {
        v1MerchantSupportService.requireEmployee(tenantId, platformUserId);

        String productCode = resolveProductCode(dto);
        Product existing = productMapper.selectOne(new LambdaQueryWrapper<Product>()
                .eq(Product::getTenantId, tenantId)
                .eq(Product::getProductCode, productCode)
                .eq(Product::getDeleted, 0));
        if (existing != null) {
            throw new BusinessException("商品编码已存在");
        }

        Product product = new Product();
        product.setTenantId(tenantId);
        product.setProductCode(productCode);
        product.setName(dto.getName());
        product.setPrice(dto.getPrice());
        product.setUnit(dto.getUnit());
        product.setCategory(dto.getCategory());
        product.setDescription(dto.getDescription());
        product.setImageUrl(dto.getImageUrl());
        product.setStatus(toProductStatus(dto.getStatus(), dto.getStock()));
        product.setDeleted(0);
        productMapper.insert(product);

        ProductStock stock = getOrCreateStock(tenantId, product.getId());
        stock.setQuantity(Math.max(dto.getStock(), 0));
        productStockMapper.updateById(stock);

        Product savedProduct = productMapper.selectById(product.getId());
        productIndexMessagePublisher.publishUpsert(savedProduct);
        return toProductVO(savedProduct, productStockMapper.selectById(stock.getId()));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public V1MerchantProductVO updateProduct(Long tenantId, Long platformUserId, Long productId, V1MerchantProductUpsertDTO dto) {
        v1MerchantSupportService.requireEmployee(tenantId, platformUserId);

        Product product = getTenantProduct(tenantId, productId);
        String productCode = resolveProductCode(dto);
        Product existing = productMapper.selectOne(new LambdaQueryWrapper<Product>()
                .eq(Product::getTenantId, tenantId)
                .eq(Product::getProductCode, productCode)
                .eq(Product::getDeleted, 0)
                .ne(Product::getId, productId));
        if (existing != null) {
            throw new BusinessException("商品编码已被其他商品使用");
        }

        product.setProductCode(productCode);
        product.setName(dto.getName());
        product.setPrice(dto.getPrice());
        product.setUnit(dto.getUnit());
        product.setCategory(dto.getCategory());
        product.setDescription(dto.getDescription());
        product.setImageUrl(dto.getImageUrl());
        product.setStatus(toProductStatus(dto.getStatus(), dto.getStock()));
        productMapper.updateById(product);

        ProductStock stock = getOrCreateStock(tenantId, productId);
        stock.setQuantity(Math.max(dto.getStock(), 0));
        productStockMapper.updateById(stock);

        Product updatedProduct = productMapper.selectById(productId);
        productIndexMessagePublisher.publishUpsert(updatedProduct);
        return toProductVO(updatedProduct, productStockMapper.selectById(stock.getId()));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteProduct(Long tenantId, Long platformUserId, Long productId) {
        v1MerchantSupportService.requireEmployee(tenantId, platformUserId);
        Product product = getTenantProduct(tenantId, productId);
        product.setDeleted(1);
        product.setStatus(0);
        productMapper.updateById(product);
        productIndexMessagePublisher.publishDelete(product);
    }

    private Product getTenantProduct(Long tenantId, Long productId) {
        Product product = productMapper.selectOne(new LambdaQueryWrapper<Product>()
                .eq(Product::getId, productId)
                .eq(Product::getTenantId, tenantId)
                .eq(Product::getDeleted, 0));
        if (product == null) {
            throw new BusinessException("商品不存在");
        }
        return product;
    }

    private ProductStock getOrCreateStock(Long tenantId, Long productId) {
        ProductStock stock = productStockMapper.selectOne(new LambdaQueryWrapper<ProductStock>()
                .eq(ProductStock::getTenantId, tenantId)
                .eq(ProductStock::getProductId, productId));
        if (stock != null) {
            return stock;
        }

        ProductStock newStock = new ProductStock();
        newStock.setTenantId(tenantId);
        newStock.setProductId(productId);
        newStock.setQuantity(0);
        newStock.setVersion(0);
        productStockMapper.insert(newStock);
        return newStock;
    }

    private Map<Long, ProductStock> loadStockMap(Long tenantId, List<Product> products) {
        List<Long> productIds = products.stream().map(Product::getId).filter(Objects::nonNull).toList();
        if (productIds.isEmpty()) {
            return Map.of();
        }
        return productStockMapper.selectList(new LambdaQueryWrapper<ProductStock>()
                        .eq(ProductStock::getTenantId, tenantId)
                        .in(ProductStock::getProductId, productIds))
                .stream()
                .collect(Collectors.toMap(ProductStock::getProductId, Function.identity(), (left, right) -> left));
    }

    /**
     * 查询当前租户所有缺货商品 ID（quantity ≤ 0）。
     *
     * <p>新建商品时 {@code getOrCreateStock} 会同步建立 {@code ProductStock} 行（quantity 默认为 0），
     * 所以缺货等价于 "stock 行存在且 quantity ≤ 0"。如果未来允许只有 product、无 stock 行的脏数据，
     * 应在此处补 NOT EXISTS 兜底。</p>
     */
    private List<Long> loadOutOfStockProductIds(Long tenantId) {
        return productStockMapper.selectList(new LambdaQueryWrapper<ProductStock>()
                        .eq(ProductStock::getTenantId, tenantId)
                        .le(ProductStock::getQuantity, 0))
                .stream()
                .map(ProductStock::getProductId)
                .filter(Objects::nonNull)
                .toList();
    }

    private V1MerchantProductVO toProductVO(Product product, ProductStock stock) {
        V1MerchantProductVO vo = new V1MerchantProductVO();
        vo.setId(product.getId());
        vo.setTenantId(product.getTenantId());
        vo.setProductCode(product.getProductCode());
        vo.setName(product.getName());
        vo.setPrice(product.getPrice());
        vo.setUnit(product.getUnit());
        vo.setCategory(product.getCategory());
        vo.setDescription(product.getDescription());
        vo.setImageUrl(product.getImageUrl());
        vo.setStock(stock == null || stock.getQuantity() == null ? 0 : stock.getQuantity());
        vo.setStatus(resolveStatus(product, stock));
        vo.setCreateTime(product.getCreateTime());
        vo.setUpdateTime(product.getUpdateTime());
        return vo;
    }

    private String resolveStatus(Product product, ProductStock stock) {
        if (product.getStatus() == null || product.getStatus() == 0) {
            return "inactive";
        }
        int quantity = stock == null || stock.getQuantity() == null ? 0 : stock.getQuantity();
        return quantity <= 0 ? "out_of_stock" : "active";
    }

    private Integer toProductStatus(String status, Integer stock) {
        if ("inactive".equalsIgnoreCase(status)) {
            return 0;
        }
        if ("out_of_stock".equalsIgnoreCase(status) && (stock == null || stock <= 0)) {
            return 1;
        }
        return 1;
    }

    private String resolveProductCode(V1MerchantProductUpsertDTO dto) {
        if (dto.getProductCode() != null && !dto.getProductCode().isBlank()) {
            return dto.getProductCode().trim();
        }
        return BizNoGenerator.generate("PRD");
    }
}
