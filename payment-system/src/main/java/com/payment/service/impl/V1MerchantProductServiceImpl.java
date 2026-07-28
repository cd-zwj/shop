package com.payment.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.payment.common.BusinessException;
import com.payment.constant.MerchantPermission;
import com.payment.dto.V1MerchantProductChangeLogVO;
import com.payment.dto.V1MerchantProductUpsertDTO;
import com.payment.dto.V1MerchantProductVO;
import com.payment.entity.Product;
import com.payment.entity.ProductChangeLog;
import com.payment.entity.Store;
import com.payment.entity.StoreProduct;
import com.payment.entity.StoreProductStock;
import com.payment.mapper.ProductChangeLogMapper;
import com.payment.mapper.ProductMapper;
import com.payment.mapper.StoreMapper;
import com.payment.mapper.StoreProductMapper;
import com.payment.mapper.StoreProductStockMapper;
import com.payment.service.StoreInventoryService;
import com.payment.service.MerchantStoreScope;
import com.payment.service.V1MerchantProductService;
import com.payment.util.BizNoGenerator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/** 商户商品管理。商品为租户级主数据，售价、上架状态和库存由门店商品承载。 */
@Service
@RequiredArgsConstructor
public class V1MerchantProductServiceImpl implements V1MerchantProductService {

    private final ProductMapper productMapper;
    private final ProductChangeLogMapper productChangeLogMapper;
    private final StoreMapper storeMapper;
    private final StoreProductMapper storeProductMapper;
    private final StoreProductStockMapper storeProductStockMapper;
    private final StoreInventoryService storeInventoryService;
    private final ProductIndexMessagePublisher productIndexMessagePublisher;
    private final MerchantStoreScopeService merchantStoreScopeService;

    @Override
    public Page<V1MerchantProductVO> listProducts(Long tenantId, Long platformUserId, Integer current, Integer size,
                                                  String search, String category, String status) {
        MerchantStoreScope scope = resolveScope(tenantId, platformUserId);
        List<Long> accessibleProductIds = accessibleProductIds(tenantId, scope);
        if (!scope.allStores() && accessibleProductIds.isEmpty()) {
            return new Page<>(current, size, 0);
        }
        Page<Product> page = productMapper.selectPage(new Page<>(current, size), new LambdaQueryWrapper<Product>()
                .eq(Product::getTenantId, tenantId)
                .eq(Product::getDeleted, 0)
                .in(!scope.allStores(), Product::getId, accessibleProductIds)
                // 商品状态筛选属于门店商品关系，不在租户级 Product 上过滤；
                // 否则任一门店下架回写 Product.status 会污染所有门店。
                .eq(category != null && !category.isBlank(), Product::getCategory, category)
                .and(search != null && !search.isBlank(), query -> query.like(Product::getName, search)
                        .or().like(Product::getProductCode, search))
                .orderByDesc(Product::getCreateTime));

        Map<Long, StoreProduct> relationMap = primaryRelations(tenantId, page.getRecords(), scope);
        List<V1MerchantProductVO> records = page.getRecords().stream()
                .map(product -> toProductVO(product, relationMap.get(product.getId())))
                .filter(vo -> status == null || status.isBlank() || status.equalsIgnoreCase(vo.getStatus()))
                .toList();
        Page<V1MerchantProductVO> result = new Page<>(page.getCurrent(), page.getSize(), page.getTotal());
        result.setRecords(records);
        return result;
    }

    @Override
    public V1MerchantProductVO getProduct(Long tenantId, Long platformUserId, Long productId) {
        MerchantStoreScope scope = resolveScope(tenantId, platformUserId);
        Product product = getTenantProduct(tenantId, productId);
        StoreProduct relation = primaryRelation(tenantId, productId, scope);
        if (!scope.allStores() && relation == null) {
            throw new BusinessException("商品不存在或无权访问");
        }
        return toProductVO(product, relation);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public V1MerchantProductVO createProduct(Long tenantId, Long platformUserId, V1MerchantProductUpsertDTO dto) {
        MerchantStoreScope scope = resolveScope(tenantId, platformUserId);
        requireAllStores(scope, "仅全门店管理员可创建租户商品");
        merchantStoreScopeService.requireStoreAccess(scope, dto.getStoreId());
        validateActiveStore(tenantId, dto.getStoreId());
        validateFulfillmentMode(dto.getFulfillmentMode());
        String productCode = resolveProductCode(dto);
        ensureProductCodeAvailable(tenantId, productCode, null);

        Product product = new Product();
        product.setTenantId(tenantId);
        product.setProductCode(productCode);
        applyProduct(product, dto);
        // Product.status 表示租户级主数据是否有效；单店上下架写 StoreProduct.status。
        // 新建主数据默认有效，即使首个门店选择 inactive，也不污染后续其他门店。
        product.setStatus(1);
        product.setDeleted(0);
        productMapper.insert(product);

        StoreProduct relation = createRelation(tenantId, dto.getStoreId(), product.getId(), dto);
        applyRequestedStock(tenantId, platformUserId, dto.getStoreId(), product.getId(), dto.getStock());
        Product savedProduct = productMapper.selectById(product.getId());
        productIndexMessagePublisher.publishUpsert(savedProduct);
        return toProductVO(savedProduct, relation);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public V1MerchantProductVO updateProduct(Long tenantId, Long platformUserId, Long productId, V1MerchantProductUpsertDTO dto) {
        MerchantStoreScope scope = resolveScope(tenantId, platformUserId);
        merchantStoreScopeService.requireStoreAccess(scope, dto.getStoreId());
        validateActiveStore(tenantId, dto.getStoreId());
        validateFulfillmentMode(dto.getFulfillmentMode());
        Product product = getTenantProduct(tenantId, productId);
        if (!scope.allStores()) {
            ensureTenantMetadataUnchanged(product, dto);
        }
        String productCode = resolveProductCode(dto);
        ensureProductCodeAvailable(tenantId, productCode, productId);

        StoreProduct relation = storeRelation(tenantId, dto.getStoreId(), productId);
        BigDecimal oldPrice = relation == null || relation.getPrice() == null ? product.getPrice() : relation.getPrice();
        int oldQuantity = currentQuantity(tenantId, dto.getStoreId(), productId);
        product.setProductCode(productCode);
        applyTenantProductMetadata(product, dto);
        // dto.price/status 是当前门店关系字段，只允许 upsertRelation 写 StoreProduct；
        // 不再覆盖租户级 Product.price/status，避免 B 店调价/下架污染 A 店。
        productMapper.updateById(product);

        relation = upsertRelation(tenantId, dto.getStoreId(), productId, dto);
        applyRequestedStock(tenantId, platformUserId, dto.getStoreId(), productId, dto.getStock());
        recordPriceAndStockChanges(tenantId, platformUserId, productId, oldPrice, relation.getPrice(), oldQuantity, dto.getStock());

        Product savedProduct = productMapper.selectById(productId);
        productIndexMessagePublisher.publishUpsert(savedProduct);
        return toProductVO(savedProduct, relation);
    }

    @Override
    public Page<V1MerchantProductChangeLogVO> listProductChangeLogs(Long tenantId, Long platformUserId, Long productId,
                                                                      Integer current, Integer size) {
        MerchantStoreScope scope = resolveScope(tenantId, platformUserId);
        requireAllStores(scope, "仅全门店管理员可查看商品变更流水");
        getTenantProduct(tenantId, productId);
        Page<ProductChangeLog> page = productChangeLogMapper.selectPage(new Page<>(current, size),
                new LambdaQueryWrapper<ProductChangeLog>()
                        .eq(ProductChangeLog::getTenantId, tenantId)
                        .eq(ProductChangeLog::getProductId, productId)
                        .orderByDesc(ProductChangeLog::getCreateTime)
                        .orderByDesc(ProductChangeLog::getId));
        Page<V1MerchantProductChangeLogVO> result = new Page<>(page.getCurrent(), page.getSize(), page.getTotal());
        result.setRecords(page.getRecords().stream().map(V1MerchantProductChangeLogVO::from).toList());
        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteProduct(Long tenantId, Long platformUserId, Long productId) {
        MerchantStoreScope scope = resolveScope(tenantId, platformUserId);
        requireAllStores(scope, "仅全门店管理员可删除租户商品");
        Product product = getTenantProduct(tenantId, productId);
        product.setDeleted(1);
        product.setStatus(0);
        productMapper.updateById(product);
        storeProductMapper.update(null, new com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<StoreProduct>()
                .eq(StoreProduct::getTenantId, tenantId)
                .eq(StoreProduct::getProductId, productId)
                .set(StoreProduct::getStatus, 0));
        productIndexMessagePublisher.publishDelete(product);
    }

    private Map<Long, StoreProduct> primaryRelations(Long tenantId, List<Product> products,
                                                     MerchantStoreScope scope) {
        List<Long> productIds = products.stream().map(Product::getId).toList();
        if (productIds.isEmpty()) {
                return Map.of();
        }
        return storeProductMapper.selectList(new LambdaQueryWrapper<StoreProduct>()
                        .eq(StoreProduct::getTenantId, tenantId)
                        .in(StoreProduct::getProductId, productIds)
                        .in(!scope.allStores(), StoreProduct::getStoreId, scope.storeIds())
                        .orderByAsc(StoreProduct::getStoreId))
                .stream()
                .collect(Collectors.toMap(StoreProduct::getProductId, relation -> relation, (left, right) -> left));
    }

    private StoreProduct storeRelation(Long tenantId, Long storeId, Long productId) {
        return storeProductMapper.selectOne(new LambdaQueryWrapper<StoreProduct>()
                .eq(StoreProduct::getTenantId, tenantId)
                .eq(StoreProduct::getStoreId, storeId)
                .eq(StoreProduct::getProductId, productId));
    }

    private StoreProduct primaryRelation(Long tenantId, Long productId, MerchantStoreScope scope) {
        return storeProductMapper.selectOne(new LambdaQueryWrapper<StoreProduct>()
                .eq(StoreProduct::getTenantId, tenantId)
                .eq(StoreProduct::getProductId, productId)
                .in(!scope.allStores(), StoreProduct::getStoreId, scope.storeIds())
                .orderByAsc(StoreProduct::getStoreId)
                .last("LIMIT 1"));
    }

    private List<Long> accessibleProductIds(Long tenantId, MerchantStoreScope scope) {
        if (scope.allStores()) {
            return List.of();
        }
        if (scope.storeIds().isEmpty()) {
            return List.of();
        }
        return storeProductMapper.selectList(new LambdaQueryWrapper<StoreProduct>()
                        .eq(StoreProduct::getTenantId, tenantId)
                        .in(StoreProduct::getStoreId, scope.storeIds()))
                .stream()
                .map(StoreProduct::getProductId)
                .filter(java.util.Objects::nonNull)
                .distinct()
                .toList();
    }

    private MerchantStoreScope resolveScope(Long tenantId, Long platformUserId) {
        return merchantStoreScopeService.resolve(tenantId, platformUserId, MerchantPermission.PRODUCT_MANAGE);
    }

    private void requireAllStores(MerchantStoreScope scope, String message) {
        if (!scope.allStores()) {
            throw new BusinessException(message);
        }
    }

    private void ensureTenantMetadataUnchanged(Product product, V1MerchantProductUpsertDTO dto) {
        String requestedCode = dto.getProductCode() == null ? null : dto.getProductCode().trim();
        if (!java.util.Objects.equals(product.getProductCode(), requestedCode)
                || !java.util.Objects.equals(product.getName(), dto.getName())
                || !java.util.Objects.equals(product.getUnit(), dto.getUnit())
                || !java.util.Objects.equals(product.getCategory(), dto.getCategory())
                || !java.util.Objects.equals(product.getDescription(), dto.getDescription())
                || !java.util.Objects.equals(product.getImageUrl(), dto.getImageUrl())) {
            throw new BusinessException("当前员工只能修改授权门店的价格、上下架状态和库存");
        }
    }

    private StoreProduct createRelation(Long tenantId, Long storeId, Long productId, V1MerchantProductUpsertDTO dto) {
        StoreProduct relation = new StoreProduct();
        relation.setTenantId(tenantId);
        relation.setStoreId(storeId);
        relation.setProductId(productId);
        relation.setPrice(dto.getPrice());
        relation.setStatus(toRelationStatus(dto.getStatus()));
        storeProductMapper.insert(relation);
        return relation;
    }

    private StoreProduct upsertRelation(Long tenantId, Long storeId, Long productId, V1MerchantProductUpsertDTO dto) {
        StoreProduct relation = storeProductMapper.selectOne(new LambdaQueryWrapper<StoreProduct>()
                .eq(StoreProduct::getTenantId, tenantId)
                .eq(StoreProduct::getStoreId, storeId)
                .eq(StoreProduct::getProductId, productId));
        if (relation == null) {
            return createRelation(tenantId, storeId, productId, dto);
        }
        relation.setPrice(dto.getPrice());
        relation.setStatus(toRelationStatus(dto.getStatus()));
        storeProductMapper.updateById(relation);
        return relation;
    }

    private void applyRequestedStock(Long tenantId, Long platformUserId, Long storeId, Long productId, Integer targetQuantity) {
        int currentQuantity = currentQuantity(tenantId, storeId, productId);
        int delta = targetQuantity - currentQuantity;
        if (delta != 0) {
            storeInventoryService.adjust(tenantId, storeId, productId, delta, platformUserId, "商品库存调整");
        }
    }

    private int currentQuantity(Long tenantId, Long storeId, Long productId) {
        StoreProductStock stock = storeProductStockMapper.selectOne(new LambdaQueryWrapper<StoreProductStock>()
                .eq(StoreProductStock::getTenantId, tenantId)
                .eq(StoreProductStock::getStoreId, storeId)
                .eq(StoreProductStock::getProductId, productId));
        return stock == null || stock.getQuantity() == null ? 0 : stock.getQuantity();
    }

    private V1MerchantProductVO toProductVO(Product product, StoreProduct relation) {
        V1MerchantProductVO vo = new V1MerchantProductVO();
        vo.setId(product.getId());
        vo.setTenantId(product.getTenantId());
        vo.setProductCode(product.getProductCode());
        vo.setName(product.getName());
        vo.setPrice(relation == null || relation.getPrice() == null ? product.getPrice() : relation.getPrice());
        vo.setUnit(product.getUnit());
        vo.setCategory(product.getCategory());
        vo.setDescription(product.getDescription());
        vo.setImageUrl(product.getImageUrl());
        vo.setStoreId(relation == null ? null : relation.getStoreId());
        vo.setFulfillmentMode("STORE_PICKUP");
        int quantity = relation == null ? 0 : currentQuantity(product.getTenantId(), relation.getStoreId(), product.getId());
        vo.setStock(quantity);
        vo.setStatus(resolveStatus(product, relation, quantity));
        vo.setCreateTime(product.getCreateTime());
        vo.setUpdateTime(product.getUpdateTime());
        return vo;
    }

    private String resolveStatus(Product product, StoreProduct relation, int quantity) {
        if (product.getStatus() == null || product.getStatus() == 0
                || relation == null || !Integer.valueOf(1).equals(relation.getStatus())) {
            return "inactive";
        }
        return quantity <= 0 ? "out_of_stock" : "active";
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

    /**
     * 更新租户级商品主数据。价格与上下架不属于该层：它们由 StoreProduct
     * 按门店承载，防止单店编辑污染同一商品在其他门店的售价和可售状态。
     */
    private void applyTenantProductMetadata(Product product, V1MerchantProductUpsertDTO dto) {
        product.setName(dto.getName());
        product.setUnit(dto.getUnit());
        product.setCategory(dto.getCategory());
        product.setDescription(dto.getDescription());
        product.setImageUrl(dto.getImageUrl());
    }

    /** 新建商品时用首店价格初始化租户级基准价，仅作为无门店关系时的兜底。 */
    private void applyProduct(Product product, V1MerchantProductUpsertDTO dto) {
        applyTenantProductMetadata(product, dto);
        product.setPrice(dto.getPrice());
    }

    private void ensureProductCodeAvailable(Long tenantId, String productCode, Long excludeProductId) {
        Product existing = productMapper.selectOne(new LambdaQueryWrapper<Product>()
                .eq(Product::getTenantId, tenantId)
                .eq(Product::getProductCode, productCode)
                .eq(Product::getDeleted, 0)
                .ne(excludeProductId != null, Product::getId, excludeProductId));
        if (existing != null) {
            throw new BusinessException("商品编码已存在");
        }
    }

    private void validateActiveStore(Long tenantId, Long storeId) {
        if (storeId == null || storeId <= 0) {
            throw new BusinessException("商品必须绑定门店");
        }
        Store store = storeMapper.selectOne(new LambdaQueryWrapper<Store>()
                .eq(Store::getId, storeId)
                .eq(Store::getTenantId, tenantId)
                .eq(Store::getDeleted, 0)
                .eq(Store::getStatus, 1));
        if (store == null) {
            throw new BusinessException("门店不存在或已停用");
        }
    }

    private void validateFulfillmentMode(String mode) {
        if (mode != null && !mode.isBlank() && !"STORE_PICKUP".equalsIgnoreCase(mode.trim())) {
            throw new BusinessException("当前仅支持到店自提履约方式");
        }
    }

    private Integer toRelationStatus(String status) {
        return "inactive".equalsIgnoreCase(status) ? 0 : 1;
    }

    private void recordPriceAndStockChanges(Long tenantId, Long platformUserId, Long productId,
                                            BigDecimal oldPrice, BigDecimal newPrice, int oldStock, int newStock) {
        if (!sameAmount(oldPrice, newPrice)) {
            recordChange(tenantId, productId, "PRICE", "price", formatAmount(oldPrice), formatAmount(newPrice),
                    platformUserId, "商户调整门店售价");
        }
        if (oldStock != newStock) {
            recordChange(tenantId, productId, "STOCK", "stock", String.valueOf(oldStock), String.valueOf(newStock),
                    platformUserId, "商户调整门店库存");
        }
    }

    private void recordChange(Long tenantId, Long productId, String changeType, String fieldName, String oldValue,
                              String newValue, Long operatorId, String remark) {
        ProductChangeLog log = new ProductChangeLog();
        log.setTenantId(tenantId);
        log.setProductId(productId);
        log.setChangeType(changeType);
        log.setFieldName(fieldName);
        log.setOldValue(oldValue);
        log.setNewValue(newValue);
        log.setOperatorId(operatorId);
        log.setRemark(remark);
        productChangeLogMapper.insert(log);
    }

    private boolean sameAmount(BigDecimal left, BigDecimal right) {
        return left == null ? right == null : right != null && left.compareTo(right) == 0;
    }

    private String formatAmount(BigDecimal amount) {
        return amount == null ? null : amount.setScale(2, RoundingMode.HALF_UP).toPlainString();
    }

    private String resolveProductCode(V1MerchantProductUpsertDTO dto) {
        return dto.getProductCode() == null || dto.getProductCode().isBlank()
                ? BizNoGenerator.generate("PRD") : dto.getProductCode().trim();
    }
}
