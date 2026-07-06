package com.payment.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.payment.common.BusinessException;
import com.payment.constant.MerchantPermission;
import com.payment.dto.V1MerchantProductUpsertDTO;
import com.payment.dto.V1MerchantProductVO;
import com.payment.entity.Product;
import com.payment.entity.ProductChangeLog;
import com.payment.entity.ProductStock;
import com.payment.entity.Store;
import com.payment.entity.VirtualProductCategory;
import com.payment.entity.VirtualProductType;
import com.payment.mapper.ProductChangeLogMapper;
import com.payment.mapper.ProductMapper;
import com.payment.mapper.ProductStockMapper;
import com.payment.mapper.StoreMapper;
import com.payment.mapper.VirtualProductCategoryMapper;
import com.payment.mapper.VirtualProductTypeMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 商户端商品管理服务测试。
 */
class V1MerchantProductServiceImplTest {

    private ProductMapper productMapper;
    private ProductStockMapper productStockMapper;
    private ProductChangeLogMapper productChangeLogMapper;
    private StoreMapper storeMapper;
    private VirtualProductTypeMapper virtualProductTypeMapper;
    private VirtualProductCategoryMapper virtualProductCategoryMapper;
    private V1MerchantSupportService supportService;
    private ProductIndexMessagePublisher productIndexMessagePublisher;
    private V1MerchantProductServiceImpl service;

    @BeforeEach
    void setUp() {
        productMapper = mock(ProductMapper.class);
        productStockMapper = mock(ProductStockMapper.class);
        productChangeLogMapper = mock(ProductChangeLogMapper.class);
        storeMapper = mock(StoreMapper.class);
        virtualProductTypeMapper = mock(VirtualProductTypeMapper.class);
        virtualProductCategoryMapper = mock(VirtualProductCategoryMapper.class);
        supportService = mock(V1MerchantSupportService.class);
        productIndexMessagePublisher = mock(ProductIndexMessagePublisher.class);
        service = new V1MerchantProductServiceImpl(productMapper, productStockMapper, productChangeLogMapper, storeMapper, virtualProductTypeMapper, virtualProductCategoryMapper, supportService, productIndexMessagePublisher);
    }

    @Test
    void listProductsShouldRequireProductPermissionAndReturnPagedVOs() {
        Product product = buildProduct(1L, 1L, "P001", "咖啡", 1);
        Page<Product> mapperPage = new Page<>(1, 10);
        mapperPage.setRecords(List.of(product));
        mapperPage.setTotal(1);
        when(productMapper.selectPage(any(), any())).thenReturn(mapperPage);
        when(productStockMapper.selectList(any())).thenReturn(List.of(buildStock(10L, 1L, 1L, 5)));

        Page<V1MerchantProductVO> result = service.listProducts(1L, 100L, 1, 10, null, null, null);

        verify(supportService).requirePermission(1L, 100L, MerchantPermission.PRODUCT_MANAGE);
        assertEquals(1, result.getRecords().size());
        assertEquals("active", result.getRecords().get(0).getStatus());
        assertEquals(5, result.getRecords().get(0).getStock());
        assertEquals(20L, result.getRecords().get(0).getStoreId());
    }

    @Test
    void listProductsShouldFilterOutOfStock() {
        // out_of_stock 模式下应：1) 先用 product_stock 锁定缺货 productId 子集；2) total 仅包含缺货数；
        // 3) 跨页时不会因内存过滤漏行。
        Product outOfStock = buildProduct(2L, 1L, "P002", "拿铁", 1);
        Page<Product> mapperPage = new Page<>(1, 10);
        mapperPage.setRecords(List.of(outOfStock));
        mapperPage.setTotal(1);
        // 第一次 selectList：找缺货 productId；第二次：loadStockMap
        when(productStockMapper.selectList(any()))
                .thenReturn(List.of(buildStock(11L, 1L, 2L, 0)))
                .thenReturn(List.of(buildStock(11L, 1L, 2L, 0)));
        when(productMapper.selectPage(any(), any())).thenReturn(mapperPage);

        Page<V1MerchantProductVO> result = service.listProducts(1L, 100L, 1, 10, null, null, "out_of_stock");

        assertEquals(1L, result.getTotal());
        assertEquals(1, result.getRecords().size());
        assertEquals(2L, result.getRecords().get(0).getId());
        assertEquals(0, result.getRecords().get(0).getStock());
    }

    @Test
    void listProductsShouldShortCircuitWhenNoOutOfStock() {
        // 该租户没有缺货商品时应直接返回空页，不再触发 product 表分页查询
        when(productStockMapper.selectList(any())).thenReturn(List.of());

        Page<V1MerchantProductVO> result = service.listProducts(1L, 100L, 1, 10, null, null, "out_of_stock");

        assertEquals(0L, result.getTotal());
        assertEquals(0, result.getRecords().size());
        verify(productMapper, never()).selectPage(any(), any());
    }

    @Test
    void listProductsOutOfStockShouldPaginateCorrectlyAcrossPages() {
        // 模拟租户共有 3 个商品全部缺货；第 2 页 size=2 应当返回 1 条，total=3
        Product p3 = buildProduct(3L, 1L, "P003", "美式", 1);
        Page<Product> mapperPage = new Page<>(2, 2);
        mapperPage.setRecords(List.of(p3));
        mapperPage.setTotal(3);
        when(productStockMapper.selectList(any()))
                .thenReturn(List.of(
                        buildStock(10L, 1L, 1L, 0),
                        buildStock(11L, 1L, 2L, 0),
                        buildStock(12L, 1L, 3L, 0)))
                .thenReturn(List.of(buildStock(12L, 1L, 3L, 0)));
        when(productMapper.selectPage(any(), any())).thenReturn(mapperPage);

        Page<V1MerchantProductVO> result = service.listProducts(1L, 100L, 2, 2, null, null, "out_of_stock");

        assertEquals(3L, result.getTotal());
        assertEquals(1, result.getRecords().size());
        assertEquals(3L, result.getRecords().get(0).getId());
    }

    @Test
    void getProductShouldReturnVO() {
        when(productMapper.selectOne(any(LambdaQueryWrapper.class)))
                .thenReturn(buildProduct(1L, 1L, "P001", "咖啡", 1));
        when(productStockMapper.selectOne(any())).thenReturn(buildStock(10L, 1L, 1L, 3));

        V1MerchantProductVO vo = service.getProduct(1L, 100L, 1L);

        verify(supportService).requirePermission(1L, 100L, MerchantPermission.PRODUCT_MANAGE);
        assertEquals(1L, vo.getId());
        assertEquals(3, vo.getStock());
        assertEquals("active", vo.getStatus());
    }

    @Test
    void getProductShouldThrowWhenProductMissing() {
        when(productMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(null);

        assertThrows(BusinessException.class, () -> service.getProduct(1L, 100L, 999L));
    }

    @Test
    void createProductShouldPersistAndPublishIndex() {
        // 第一次 selectOne 查重 -> 不存在
        when(productMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(null);
        // 库存初始化时 selectOne 返回 null -> 走 insert 分支
        when(productStockMapper.selectOne(any())).thenReturn(null);
        when(productMapper.selectById(any())).thenReturn(buildProduct(1L, 1L, "P001", "咖啡", 1));
        when(productStockMapper.selectById(any())).thenReturn(buildStock(10L, 1L, 1L, 8));

        V1MerchantProductUpsertDTO dto = buildUpsertDTO("P001", "咖啡", 8, "active");
        dto.setStoreId(20L);
        when(storeMapper.selectOne(any())).thenReturn(buildStore(20L, 1L, 1));
        V1MerchantProductVO vo = service.createProduct(1L, 100L, dto);

        verify(supportService).requirePermission(1L, 100L, MerchantPermission.PRODUCT_MANAGE);
        verify(productMapper).insert(any(Product.class));
        verify(productStockMapper).insert(any(ProductStock.class));

        ArgumentCaptor<ProductStock> stockCaptor = ArgumentCaptor.forClass(ProductStock.class);
        verify(productStockMapper).updateById(stockCaptor.capture());
        assertEquals(8, stockCaptor.getValue().getQuantity());

        verify(productIndexMessagePublisher).publishUpsert(any(Product.class));
        assertEquals("active", vo.getStatus());
        assertEquals(20L, vo.getStoreId());
    }

    @Test
    void createProductShouldRejectStoreFromOtherTenant() {
        when(productMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(null);
        when(storeMapper.selectOne(any())).thenReturn(null);

        V1MerchantProductUpsertDTO dto = buildUpsertDTO("P001", "咖啡", 8, "active");
        dto.setStoreId(20L);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.createProduct(1L, 100L, dto));
        assertEquals("门店不存在或已停用", ex.getMessage());
        verify(productMapper, never()).insert(any(Product.class));
    }

    @Test
    void createProductShouldRejectDuplicateCode() {
        when(productMapper.selectOne(any(LambdaQueryWrapper.class)))
                .thenReturn(buildProduct(99L, 1L, "P001", "已存在", 1));

        V1MerchantProductUpsertDTO dto = buildUpsertDTO("P001", "咖啡", 8, "active");
        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.createProduct(1L, 100L, dto));
        assertEquals("商品编码已存在", ex.getMessage());
        verify(productMapper, never()).insert(any(Product.class));
        verify(productIndexMessagePublisher, never()).publishUpsert(any());
    }

    @Test
    void createProductShouldClampNegativeStockToZero() {
        when(productMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(null);
        when(productStockMapper.selectOne(any())).thenReturn(null);
        when(productMapper.selectById(any())).thenReturn(buildProduct(1L, 1L, "P001", "咖啡", 1));
        when(productStockMapper.selectById(any())).thenReturn(buildStock(10L, 1L, 1L, 0));

        V1MerchantProductUpsertDTO dto = buildUpsertDTO("P001", "咖啡", -5, "active");
        service.createProduct(1L, 100L, dto);

        ArgumentCaptor<ProductStock> stockCaptor = ArgumentCaptor.forClass(ProductStock.class);
        verify(productStockMapper).updateById(stockCaptor.capture());
        assertEquals(0, stockCaptor.getValue().getQuantity());
    }

    @Test
    void updateProductShouldRejectDuplicateCodeOnOtherProduct() {
        // 第一次 selectOne 是获取目标商品；第二次 selectOne 是查重
        when(productMapper.selectOne(any(LambdaQueryWrapper.class)))
                .thenReturn(buildProduct(1L, 1L, "P001", "咖啡", 1))
                .thenReturn(buildProduct(2L, 1L, "P002", "占用编码的其他商品", 1));

        V1MerchantProductUpsertDTO dto = buildUpsertDTO("P002", "咖啡", 5, "active");
        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.updateProduct(1L, 100L, 1L, dto));
        assertEquals("商品编码已被其他商品使用", ex.getMessage());
        verify(productMapper, never()).updateById(any(Product.class));
    }

    @Test
    void updateProductShouldPersistAndPublishIndex() {
        when(productMapper.selectOne(any(LambdaQueryWrapper.class)))
                .thenReturn(buildProduct(1L, 1L, "P001", "咖啡", 1))
                .thenReturn(null);
        when(productStockMapper.selectOne(any())).thenReturn(buildStock(10L, 1L, 1L, 0));
        when(productMapper.selectById(any())).thenReturn(buildProduct(1L, 1L, "P001", "咖啡升级", 1));
        when(productStockMapper.selectById(any())).thenReturn(buildStock(10L, 1L, 1L, 12));
        when(storeMapper.selectOne(any())).thenReturn(buildStore(30L, 1L, 1));

        V1MerchantProductUpsertDTO dto = buildUpsertDTO("P001", "咖啡升级", 12, "active");
        dto.setStoreId(30L);
        V1MerchantProductVO vo = service.updateProduct(1L, 100L, 1L, dto);

        ArgumentCaptor<Product> productCaptor = ArgumentCaptor.forClass(Product.class);
        verify(productMapper).updateById(productCaptor.capture());
        assertEquals(30L, productCaptor.getValue().getStoreId());
        verify(productIndexMessagePublisher).publishUpsert(any(Product.class));
        assertEquals("active", vo.getStatus());
    }

    @Test
    void updateProductShouldRecordPriceAndStockChanges() {
        Product existing = buildProduct(1L, 1L, "P001", "咖啡", 1);
        existing.setPrice(new BigDecimal("28.00"));
        when(productMapper.selectOne(any(LambdaQueryWrapper.class)))
                .thenReturn(existing)
                .thenReturn(null);
        when(productStockMapper.selectOne(any())).thenReturn(buildStock(10L, 1L, 1L, 5));
        Product updated = buildProduct(1L, 1L, "P001", "咖啡升级", 1);
        updated.setPrice(new BigDecimal("32.00"));
        when(productMapper.selectById(any())).thenReturn(updated);
        when(productStockMapper.selectById(any())).thenReturn(buildStock(10L, 1L, 1L, 8));

        V1MerchantProductUpsertDTO dto = buildUpsertDTO("P001", "咖啡升级", 8, "active");
        dto.setPrice(new BigDecimal("32.00"));
        service.updateProduct(1L, 100L, 1L, dto);

        ArgumentCaptor<ProductChangeLog> captor = ArgumentCaptor.forClass(ProductChangeLog.class);
        verify(productChangeLogMapper, times(2)).insert(captor.capture());

        List<ProductChangeLog> logs = captor.getAllValues();
        assertEquals("PRICE", logs.get(0).getChangeType());
        assertEquals("price", logs.get(0).getFieldName());
        assertEquals("28.00", logs.get(0).getOldValue());
        assertEquals("32.00", logs.get(0).getNewValue());
        assertEquals(100L, logs.get(0).getOperatorId());

        assertEquals("STOCK", logs.get(1).getChangeType());
        assertEquals("stock", logs.get(1).getFieldName());
        assertEquals("5", logs.get(1).getOldValue());
        assertEquals("8", logs.get(1).getNewValue());
        assertEquals(1L, logs.get(1).getProductId());
    }

    @Test
    void updateProductShouldNotRecordChangeWhenPriceAndStockUnchanged() {
        Product existing = buildProduct(1L, 1L, "P001", "咖啡", 1);
        existing.setPrice(new BigDecimal("28.00"));
        when(productMapper.selectOne(any(LambdaQueryWrapper.class)))
                .thenReturn(existing)
                .thenReturn(null);
        when(productStockMapper.selectOne(any())).thenReturn(buildStock(10L, 1L, 1L, 5));
        when(productMapper.selectById(any())).thenReturn(existing);
        when(productStockMapper.selectById(any())).thenReturn(buildStock(10L, 1L, 1L, 5));

        V1MerchantProductUpsertDTO dto = buildUpsertDTO("P001", "咖啡", 5, "active");
        service.updateProduct(1L, 100L, 1L, dto);

        verify(productChangeLogMapper, never()).insert(any(ProductChangeLog.class));
    }

    @Test
    void listProductChangeLogsShouldRequireEmployeeAndReturnPagedLogs() {
        ProductChangeLog log = new ProductChangeLog();
        log.setId(7L);
        log.setTenantId(1L);
        log.setProductId(1L);
        log.setChangeType("PRICE");
        log.setFieldName("price");
        log.setOldValue("28.00");
        log.setNewValue("32.00");
        log.setOperatorId(100L);

        Page<ProductChangeLog> mapperPage = new Page<>(1, 10);
        mapperPage.setRecords(List.of(log));
        mapperPage.setTotal(1);
        when(productMapper.selectOne(any(LambdaQueryWrapper.class)))
                .thenReturn(buildProduct(1L, 1L, "P001", "咖啡", 1));
        when(productChangeLogMapper.selectPage(any(), any())).thenReturn(mapperPage);

        Page<?> result = service.listProductChangeLogs(1L, 100L, 1L, 1, 10);

        verify(supportService).requirePermission(1L, 100L, MerchantPermission.PRODUCT_MANAGE);
        assertEquals(1L, result.getTotal());
        assertEquals(1, result.getRecords().size());
    }

    @Test
    void deleteProductShouldMarkDeletedAndPublishIndex() {
        Product existing = buildProduct(1L, 1L, "P001", "咖啡", 1);
        when(productMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(existing);

        service.deleteProduct(1L, 100L, 1L);

        ArgumentCaptor<Product> captor = ArgumentCaptor.forClass(Product.class);
        verify(productMapper).updateById(captor.capture());
        assertEquals(1, captor.getValue().getDeleted());
        assertEquals(0, captor.getValue().getStatus());
        verify(productIndexMessagePublisher, times(1)).publishDelete(any(Product.class));
    }

    @Test
    void writesShouldFailWhenProductPermissionRejected() {
        // requirePermission 抛出业务异常时，写路径应短路
        org.mockito.Mockito.doThrow(new BusinessException("当前用户无权访问该商户"))
                .when(supportService).requirePermission(any(), any(), any());

        V1MerchantProductUpsertDTO dto = buildUpsertDTO("P001", "咖啡", 1, "active");
        assertThrows(BusinessException.class, () -> service.createProduct(1L, 100L, dto));
        assertThrows(BusinessException.class, () -> service.updateProduct(1L, 100L, 1L, dto));
        assertThrows(BusinessException.class, () -> service.deleteProduct(1L, 100L, 1L));

        verify(productMapper, never()).insert(any(Product.class));
        verify(productMapper, never()).updateById(any(Product.class));
        verify(productIndexMessagePublisher, never()).publishUpsert(any());
        verify(productIndexMessagePublisher, never()).publishDelete(any());
    }

    @Test
    void createProductShouldRejectPhysicalWithVirtualTaxonomy() {
        when(productMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(null);
        V1MerchantProductUpsertDTO dto = buildUpsertDTO("P001", "咖啡", 8, "active");
        dto.setProductType("PHYSICAL");
        dto.setVirtualTypeId(1L);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.createProduct(1L, 100L, dto));

        assertEquals("实物商品不允许绑定虚拟商品类型或分类", ex.getMessage());
        verify(productMapper, never()).insert(any(Product.class));
    }

    @Test
    void createProductShouldRequireVirtualTypeForOnlineVirtualProduct() {
        when(productMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(null);
        V1MerchantProductUpsertDTO dto = buildUpsertDTO("P001", "资料包", 8, "active");
        dto.setProductType("VIRTUAL");

        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.createProduct(1L, 100L, dto));

        assertEquals("虚拟商品必须绑定虚拟商品类型", ex.getMessage());
        verify(productMapper, never()).insert(any(Product.class));
    }

    @Test
    void createProductShouldRejectVirtualTypeStrategyMismatch() {
        when(productMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(null);
        when(virtualProductTypeMapper.selectOne(any())).thenReturn(buildVirtualType(1L, 1L, "CARD_KEY"));
        V1MerchantProductUpsertDTO dto = buildUpsertDTO("P001", "资料包", 8, "active");
        dto.setProductType("VIRTUAL");
        dto.setVirtualTypeId(1L);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.createProduct(1L, 100L, dto));

        assertEquals("虚拟商品类型交付策略必须和商品类型一致", ex.getMessage());
        verify(productMapper, never()).insert(any(Product.class));
    }

    @Test
    void createProductShouldRejectVirtualCategoryTypeMismatch() {
        when(productMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(null);
        when(virtualProductTypeMapper.selectOne(any())).thenReturn(buildVirtualType(1L, 1L, "VIRTUAL"));
        when(virtualProductCategoryMapper.selectOne(any())).thenReturn(buildVirtualCategory(2L, 1L, 99L));
        V1MerchantProductUpsertDTO dto = buildUpsertDTO("P001", "资料包", 8, "active");
        dto.setProductType("VIRTUAL");
        dto.setVirtualTypeId(1L);
        dto.setVirtualCategoryId(2L);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.createProduct(1L, 100L, dto));

        assertEquals("虚拟商品分类必须属于所选虚拟商品类型", ex.getMessage());
        verify(productMapper, never()).insert(any(Product.class));
    }

    private Product buildProduct(Long id, Long tenantId, String code, String name, Integer status) {
        Product product = new Product();
        product.setId(id);
        product.setTenantId(tenantId);
        product.setProductCode(code);
        product.setName(name);
        product.setPrice(new BigDecimal("28.00"));
        product.setUnit("杯");
        product.setCategory("饮品");
        product.setStoreId(20L);
        product.setStatus(status);
        product.setDeleted(0);
        return product;
    }

    private ProductStock buildStock(Long id, Long tenantId, Long productId, Integer quantity) {
        ProductStock stock = new ProductStock();
        stock.setId(id);
        stock.setTenantId(tenantId);
        stock.setProductId(productId);
        stock.setQuantity(quantity);
        stock.setVersion(0);
        return stock;
    }

    private V1MerchantProductUpsertDTO buildUpsertDTO(String code, String name, Integer stock, String status) {
        V1MerchantProductUpsertDTO dto = new V1MerchantProductUpsertDTO();
        dto.setProductCode(code);
        dto.setName(name);
        dto.setPrice(new BigDecimal("28.00"));
        dto.setUnit("杯");
        dto.setCategory("饮品");
        dto.setStock(stock);
        dto.setStatus(status);
        return dto;
    }

    private Store buildStore(Long id, Long tenantId, Integer status) {
        Store store = new Store();
        store.setId(id);
        store.setTenantId(tenantId);
        store.setStatus(status);
        store.setDeleted(0);
        return store;
    }
    private VirtualProductType buildVirtualType(Long id, Long tenantId, String deliveryStrategy) {
        VirtualProductType type = new VirtualProductType();
        type.setId(id);
        type.setTenantId(tenantId);
        type.setDeliveryStrategy(deliveryStrategy);
        type.setStatus(1);
        type.setDeleted(0);
        return type;
    }

    private VirtualProductCategory buildVirtualCategory(Long id, Long tenantId, Long typeId) {
        VirtualProductCategory category = new VirtualProductCategory();
        category.setId(id);
        category.setTenantId(tenantId);
        category.setTypeId(typeId);
        category.setStatus(1);
        category.setDeleted(0);
        return category;
    }

}
