package com.payment.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.databind.JsonNode;
import com.payment.common.BusinessException;
import com.payment.constant.MerchantPermission;
import com.payment.dto.V1MerchantProductChangeLogVO;
import com.payment.dto.V1MerchantProductUpsertDTO;
import com.payment.dto.V1MerchantProductVO;
import com.payment.entity.Product;
import com.payment.entity.ProductChangeLog;
import com.payment.entity.ProductStock;
import com.payment.entity.Store;
import com.payment.entity.VirtualProductCategory;
import com.payment.entity.VirtualProductType;
import com.payment.enums.ProductTypeEnum;
import com.payment.mapper.ProductChangeLogMapper;
import com.payment.mapper.ProductMapper;
import com.payment.mapper.ProductStockMapper;
import com.payment.mapper.StoreMapper;
import com.payment.mapper.VirtualProductCategoryMapper;
import com.payment.mapper.VirtualProductTypeMapper;
import com.payment.service.V1MerchantProductService;
import com.payment.util.BizNoGenerator;
import com.payment.util.JsonUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
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
    private final ProductChangeLogMapper productChangeLogMapper;
    private final StoreMapper storeMapper;
    private final VirtualProductTypeMapper virtualProductTypeMapper;
    private final VirtualProductCategoryMapper virtualProductCategoryMapper;
    private final V1MerchantSupportService v1MerchantSupportService;
    private final ProductIndexMessagePublisher productIndexMessagePublisher;

    @Override
    public Page<V1MerchantProductVO> listProducts(Long tenantId, Long platformUserId, Integer current, Integer size,
                                                  String search, String category, String status) {
        v1MerchantSupportService.requirePermission(tenantId, platformUserId, MerchantPermission.PRODUCT_MANAGE);

        boolean filterOutOfStock = "out_of_stock".equalsIgnoreCase(status);
        // 缺货过滤需要先按库存表锁定 productId 子集，再让 product 分页查询使用，
        // 保证 total 计入“缺货商品总数”而非“全部商品总数”，跨页也不会漏。
        List<Long> outOfStockProductIds = filterOutOfStock ? loadOutOfStockProductIds(tenantId) : List.of();
        if (filterOutOfStock && outOfStockProductIds.isEmpty()) {
            // 该租户无缺货商品时直接返回空页，避免向数据访问层传空集合触发 in() 全表扫描。
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
        v1MerchantSupportService.requirePermission(tenantId, platformUserId, MerchantPermission.PRODUCT_MANAGE);
        Product product = getTenantProduct(tenantId, productId);
        ProductStock stock = getOrCreateStock(tenantId, productId);
        return toProductVO(product, stock);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public V1MerchantProductVO createProduct(Long tenantId, Long platformUserId, V1MerchantProductUpsertDTO dto) {
        v1MerchantSupportService.requirePermission(tenantId, platformUserId, MerchantPermission.PRODUCT_MANAGE);

        String productCode = resolveProductCode(dto);
        Product existing = productMapper.selectOne(new LambdaQueryWrapper<Product>()
                .eq(Product::getTenantId, tenantId)
                .eq(Product::getProductCode, productCode)
                .eq(Product::getDeleted, 0));
        if (existing != null) {
            throw new BusinessException("商品编码已存在");
        }
        validateActiveStore(tenantId, dto.getStoreId());
        String productType = resolveProductType(dto.getProductType());
        String fulfillmentMode = resolveFulfillmentMode(productType, dto.getFulfillmentMode());
        validateVirtualTaxonomy(tenantId, productType, dto.getVirtualTypeId(), dto.getVirtualCategoryId());
        validateDeliveryConfig(productType, dto.getDeliveryConfig());

        Product product = new Product();
        product.setTenantId(tenantId);
        product.setProductCode(productCode);
        product.setName(dto.getName());
        product.setPrice(dto.getPrice());
        product.setUnit(dto.getUnit());
        product.setCategory(dto.getCategory());
        product.setDescription(dto.getDescription());
        product.setImageUrl(dto.getImageUrl());
        product.setStoreId(dto.getStoreId());
        product.setFulfillmentMode(fulfillmentMode);
        product.setVirtualTypeId(dto.getVirtualTypeId());
        product.setVirtualCategoryId(dto.getVirtualCategoryId());
        product.setStatus(toProductStatus(dto.getStatus(), dto.getStock()));
        product.setProductType(productType);
        product.setDeliveryConfig(dto.getDeliveryConfig());
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
        v1MerchantSupportService.requirePermission(tenantId, platformUserId, MerchantPermission.PRODUCT_MANAGE);

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
        validateActiveStore(tenantId, dto.getStoreId());
        String productType = resolveProductType(dto.getProductType());
        String fulfillmentMode = resolveFulfillmentMode(productType, dto.getFulfillmentMode());
        validateVirtualTaxonomy(tenantId, productType, dto.getVirtualTypeId(), dto.getVirtualCategoryId());
        validateDeliveryConfig(productType, dto.getDeliveryConfig());
        ProductStock existingStock = getOrCreateStock(tenantId, productId);
        BigDecimal oldPrice = product.getPrice();
        Integer oldStock = existingStock.getQuantity() == null ? 0 : existingStock.getQuantity();

        product.setProductCode(productCode);
        product.setName(dto.getName());
        product.setPrice(dto.getPrice());
        product.setUnit(dto.getUnit());
        product.setCategory(dto.getCategory());
        product.setDescription(dto.getDescription());
        product.setImageUrl(dto.getImageUrl());
        product.setStoreId(dto.getStoreId());
        product.setFulfillmentMode(fulfillmentMode);
        product.setVirtualTypeId(dto.getVirtualTypeId());
        product.setVirtualCategoryId(dto.getVirtualCategoryId());
        product.setStatus(toProductStatus(dto.getStatus(), dto.getStock()));
        product.setProductType(productType);
        product.setDeliveryConfig(dto.getDeliveryConfig());
        productMapper.updateById(product);

        ProductStock stock = existingStock;
        stock.setQuantity(Math.max(dto.getStock(), 0));
        productStockMapper.updateById(stock);
        recordPriceAndStockChanges(tenantId, platformUserId, productId, oldPrice, product.getPrice(), oldStock, stock.getQuantity());

        Product updatedProduct = productMapper.selectById(productId);
        productIndexMessagePublisher.publishUpsert(updatedProduct);
        return toProductVO(updatedProduct, productStockMapper.selectById(stock.getId()));
    }

    @Override
    public Page<V1MerchantProductChangeLogVO> listProductChangeLogs(Long tenantId, Long platformUserId, Long productId,
                                                                    Integer current, Integer size) {
        v1MerchantSupportService.requirePermission(tenantId, platformUserId, MerchantPermission.PRODUCT_MANAGE);
        getTenantProduct(tenantId, productId);

        Page<ProductChangeLog> page = productChangeLogMapper.selectPage(
                new Page<>(current, size),
                new LambdaQueryWrapper<ProductChangeLog>()
                        .eq(ProductChangeLog::getTenantId, tenantId)
                        .eq(ProductChangeLog::getProductId, productId)
                        .orderByDesc(ProductChangeLog::getCreateTime)
                        .orderByDesc(ProductChangeLog::getId));

        Page<V1MerchantProductChangeLogVO> result = new Page<>(page.getCurrent(), page.getSize(), page.getTotal());
        result.setRecords(page.getRecords().stream()
                .map(V1MerchantProductChangeLogVO::from)
                .toList());
        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteProduct(Long tenantId, Long platformUserId, Long productId) {
        v1MerchantSupportService.requirePermission(tenantId, platformUserId, MerchantPermission.PRODUCT_MANAGE);
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
     * 所以缺货等价于“stock 行存在且 quantity ≤ 0”。如果未来允许只有 product、无 stock 行的脏数据，
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
        vo.setStoreId(product.getStoreId());
        vo.setFulfillmentMode(product.getFulfillmentMode() == null
                ? defaultFulfillmentMode(product.getProductType())
                : product.getFulfillmentMode());
        vo.setVirtualTypeId(product.getVirtualTypeId());
        vo.setVirtualCategoryId(product.getVirtualCategoryId());
        vo.setStock(stock == null || stock.getQuantity() == null ? 0 : stock.getQuantity());
        vo.setStatus(resolveStatus(product, stock));
        vo.setProductType(product.getProductType());
        vo.setDeliveryConfig(product.getDeliveryConfig());
        vo.setCreateTime(product.getCreateTime());
        vo.setUpdateTime(product.getUpdateTime());
        return vo;
    }

    private void recordPriceAndStockChanges(Long tenantId, Long platformUserId, Long productId,
                                            BigDecimal oldPrice, BigDecimal newPrice,
                                            Integer oldStock, Integer newStock) {
        if (!sameAmount(oldPrice, newPrice)) {
            productChangeLogMapper.insert(buildChangeLog(
                    tenantId,
                    productId,
                    "PRICE",
                    "price",
                    formatAmount(oldPrice),
                    formatAmount(newPrice),
                    platformUserId,
                    "商户调整商品售价"));
        }

        int previousStock = oldStock == null ? 0 : oldStock;
        int currentStock = newStock == null ? 0 : newStock;
        if (previousStock != currentStock) {
            productChangeLogMapper.insert(buildChangeLog(
                    tenantId,
                    productId,
                    "STOCK",
                    "stock",
                    String.valueOf(previousStock),
                    String.valueOf(currentStock),
                    platformUserId,
                    "商户调整商品库存"));
        }
    }

    private ProductChangeLog buildChangeLog(Long tenantId, Long productId, String changeType, String fieldName,
                                            String oldValue, String newValue, Long operatorId, String remark) {
        ProductChangeLog log = new ProductChangeLog();
        log.setTenantId(tenantId);
        log.setProductId(productId);
        log.setChangeType(changeType);
        log.setFieldName(fieldName);
        log.setOldValue(oldValue);
        log.setNewValue(newValue);
        log.setOperatorId(operatorId);
        log.setRemark(remark);
        return log;
    }

    private boolean sameAmount(BigDecimal left, BigDecimal right) {
        if (left == null || right == null) {
            return left == right;
        }
        return left.compareTo(right) == 0;
    }

    private String formatAmount(BigDecimal amount) {
        return amount == null ? null : amount.setScale(2, RoundingMode.HALF_UP).toPlainString();
    }

    private String resolveStatus(Product product, ProductStock stock) {
        if (product.getStatus() == null || product.getStatus() == 0) {
            return "inactive";
        }
        int quantity = stock == null || stock.getQuantity() == null ? 0 : stock.getQuantity();
        return quantity <= 0 ? "out_of_stock" : "active";
    }

    /**
     * 入参 productType 字符串校验：非法值或空值兜底为 PHYSICAL，避免商户端误传破坏交付路由。
     */
    private String resolveProductType(String raw) {
        if (raw == null || raw.isBlank()) {
            return ProductTypeEnum.PHYSICAL.name();
        }
        try {
            return ProductTypeEnum.valueOf(raw.trim().toUpperCase()).name();
        } catch (IllegalArgumentException ex) {
            throw new BusinessException("不支持的商品类型: " + raw);
        }
    }

    private String resolveFulfillmentMode(String productType, String raw) {
        String mode = raw == null || raw.isBlank()
                ? defaultFulfillmentMode(productType)
                : raw.trim().toUpperCase();
        if (!"ONLINE_VIRTUAL".equals(mode) && !"OFFLINE_SERVICE".equals(mode) && !"EXPRESS_DELIVERY".equals(mode)) {
            throw new BusinessException("不支持的履约形态: " + raw);
        }
        if (ProductTypeEnum.PHYSICAL.name().equals(productType) && !"EXPRESS_DELIVERY".equals(mode)) {
            throw new BusinessException("实物商品必须使用快递发货履约形态");
        }
        if (ProductTypeEnum.SERVICE.name().equals(productType) && !"OFFLINE_SERVICE".equals(mode)) {
            throw new BusinessException("服务商品必须使用线下服务履约形态");
        }
        if (isOnlineVirtualProduct(productType) && !"ONLINE_VIRTUAL".equals(mode)) {
            throw new BusinessException("虚拟商品必须使用线上虚拟履约形态");
        }
        return mode;
    }

    private String defaultFulfillmentMode(String productType) {
        if (ProductTypeEnum.SERVICE.name().equals(productType)) {
            return "OFFLINE_SERVICE";
        }
        if (isOnlineVirtualProduct(productType)) {
            return "ONLINE_VIRTUAL";
        }
        return "EXPRESS_DELIVERY";
    }

    private boolean isOnlineVirtualProduct(String productType) {
        return ProductTypeEnum.VIRTUAL.name().equals(productType)
                || ProductTypeEnum.CARD_KEY.name().equals(productType)
                || ProductTypeEnum.SUBSCRIPTION.name().equals(productType);
    }

    private void validateVirtualTaxonomy(Long tenantId, String productType, Long virtualTypeId, Long virtualCategoryId) {
        if (ProductTypeEnum.PHYSICAL.name().equals(productType)) {
            if (virtualTypeId != null || virtualCategoryId != null) {
                throw new BusinessException("实物商品不允许绑定虚拟商品类型或分类");
            }
            return;
        }
        if (isOnlineVirtualProduct(productType) && virtualTypeId == null) {
            throw new BusinessException("虚拟商品必须绑定虚拟商品类型");
        }
        if (virtualTypeId == null) {
            if (virtualCategoryId != null) {
                throw new BusinessException("虚拟商品分类必须跟随虚拟商品类型选择");
            }
            return;
        }

        VirtualProductType type = virtualProductTypeMapper.selectOne(new LambdaQueryWrapper<VirtualProductType>()
                .eq(VirtualProductType::getId, virtualTypeId)
                .eq(VirtualProductType::getTenantId, tenantId)
                .eq(VirtualProductType::getDeleted, 0)
                .eq(VirtualProductType::getStatus, 1));
        if (type == null) {
            throw new BusinessException("虚拟商品类型不存在或已停用");
        }
        if (!productType.equals(type.getDeliveryStrategy())) {
            throw new BusinessException("虚拟商品类型交付策略必须和商品类型一致");
        }
        if (virtualCategoryId == null) {
            return;
        }

        VirtualProductCategory category = virtualProductCategoryMapper.selectOne(new LambdaQueryWrapper<VirtualProductCategory>()
                .eq(VirtualProductCategory::getId, virtualCategoryId)
                .eq(VirtualProductCategory::getTenantId, tenantId)
                .eq(VirtualProductCategory::getDeleted, 0)
                .eq(VirtualProductCategory::getStatus, 1));
        if (category == null) {
            throw new BusinessException("虚拟商品分类不存在或已停用");
        }
        if (!virtualTypeId.equals(category.getTypeId())) {
            throw new BusinessException("虚拟商品分类必须属于所选虚拟商品类型");
        }
    }

    private void validateDeliveryConfig(String productType, String deliveryConfig) {
        if (!ProductTypeEnum.VIRTUAL.name().equals(productType)) {
            return;
        }
        if (deliveryConfig == null || deliveryConfig.isBlank()) {
            throw new BusinessException("虚拟商品交付配置必须包含 contentUrl 或 accountInfo");
        }

        JsonNode node;
        try {
            node = JsonUtils.fromJsonTree(deliveryConfig);
        } catch (IllegalArgumentException ex) {
            throw new BusinessException("虚拟商品交付配置必须是合法 JSON");
        }
        if (node == null || !node.isObject()) {
            throw new BusinessException("虚拟商品交付配置必须是 JSON 对象");
        }
        boolean hasContentUrl = hasTextNode(node, "contentUrl");
        boolean hasAccountInfo = hasTextNode(node, "accountInfo");
        if (!hasContentUrl && !hasAccountInfo) {
            throw new BusinessException("虚拟商品交付配置必须包含 contentUrl 或 accountInfo");
        }
    }

    private boolean hasTextNode(JsonNode node, String fieldName) {
        JsonNode value = node.get(fieldName);
        return value != null && value.isTextual() && !value.asText().isBlank();
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

    private void validateActiveStore(Long tenantId, Long storeId) {
        if (storeId == null) {
            return;
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

    private String resolveProductCode(V1MerchantProductUpsertDTO dto) {
        if (dto.getProductCode() != null && !dto.getProductCode().isBlank()) {
            return dto.getProductCode().trim();
        }
        return BizNoGenerator.generate("PRD");
    }
}
