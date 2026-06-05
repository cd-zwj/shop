package com.payment.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.payment.common.BusinessException;
import com.payment.common.PageResult;
import com.payment.common.Result;
import com.payment.dto.V1MerchantProductUpsertDTO;
import com.payment.dto.V1MerchantProductVO;
import com.payment.entity.Product;
import com.payment.entity.ProductStock;
import com.payment.mapper.ProductMapper;
import com.payment.mapper.ProductStockMapper;
import com.payment.service.impl.ProductIndexMessagePublisher;
import com.payment.service.impl.V1MerchantSupportService;
import com.payment.util.BizNoGenerator;
import com.payment.util.PlatformSessionHelper;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/v1/merchant/tenants/{tenantId}/products")
@RequiredArgsConstructor
public class V1MerchantProductController {

    private final ProductMapper productMapper;
    private final ProductStockMapper productStockMapper;
    private final V1MerchantSupportService v1MerchantSupportService;
    private final ProductIndexMessagePublisher productIndexMessagePublisher;

    @GetMapping
    public Result<PageResult<V1MerchantProductVO>> listProducts(@PathVariable Long tenantId,
                                                          @RequestParam(defaultValue = "1") Integer current,
                                                          @RequestParam(defaultValue = "10") Integer size,
                                                          @RequestParam(required = false) String search,
                                                          @RequestParam(required = false) String category,
                                                          @RequestParam(required = false) String status) {
        v1MerchantSupportService.requireEmployee(tenantId, PlatformSessionHelper.getPlatformUserId());

        Page<Product> entityPage = new Page<>(current, size);
        LambdaQueryWrapper<Product> wrapper = new LambdaQueryWrapper<Product>()
                .eq(Product::getTenantId, tenantId)
                .eq(Product::getDeleted, 0)
                .eq("active".equalsIgnoreCase(status) || "inactive".equalsIgnoreCase(status), Product::getStatus, "active".equalsIgnoreCase(status) ? 1 : 0)
                .eq(category != null && !category.isBlank(), Product::getCategory, category)
                .and(search != null && !search.isBlank(), q -> q.like(Product::getName, search).or().like(Product::getProductCode, search))
                .orderByDesc(Product::getCreateTime);

        Page<Product> page = productMapper.selectPage(entityPage, wrapper);
        Map<Long, ProductStock> stockMap = loadStockMap(tenantId, page.getRecords());

        List<V1MerchantProductVO> records = page.getRecords().stream()
                .map(product -> toProductVO(product, stockMap.get(product.getId())))
                .filter(item -> !"out_of_stock".equalsIgnoreCase(status) || item.getStock() == 0)
                .toList();

        return Result.success(new PageResult<>(records, page.getTotal(), (int) page.getCurrent(), (int) page.getSize()));
    }

    @GetMapping("/{productId}")
    public Result<V1MerchantProductVO> getProduct(@PathVariable Long tenantId, @PathVariable Long productId) {
        v1MerchantSupportService.requireEmployee(tenantId, PlatformSessionHelper.getPlatformUserId());
        Product product = getTenantProduct(tenantId, productId);
        ProductStock stock = getOrCreateStock(tenantId, productId);
        return Result.success(toProductVO(product, stock));
    }

    @PostMapping
    public Result<V1MerchantProductVO> createProduct(@PathVariable Long tenantId,
                                                     @Valid @RequestBody V1MerchantProductUpsertDTO dto) {
        v1MerchantSupportService.requireEmployee(tenantId, PlatformSessionHelper.getPlatformUserId());

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
        return Result.success(toProductVO(savedProduct, productStockMapper.selectById(stock.getId())));
    }

    @PutMapping("/{productId}")
    public Result<V1MerchantProductVO> updateProduct(@PathVariable Long tenantId,
                                                     @PathVariable Long productId,
                                                     @Valid @RequestBody V1MerchantProductUpsertDTO dto) {
        v1MerchantSupportService.requireEmployee(tenantId, PlatformSessionHelper.getPlatformUserId());

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
        return Result.success(toProductVO(updatedProduct, productStockMapper.selectById(stock.getId())));
    }

    @DeleteMapping("/{productId}")
    public Result<Void> deleteProduct(@PathVariable Long tenantId, @PathVariable Long productId) {
        v1MerchantSupportService.requireEmployee(tenantId, PlatformSessionHelper.getPlatformUserId());
        Product product = getTenantProduct(tenantId, productId);
        product.setDeleted(1);
        product.setStatus(0);
        productMapper.updateById(product);
        productIndexMessagePublisher.publishDelete(product);
        return Result.success();
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
