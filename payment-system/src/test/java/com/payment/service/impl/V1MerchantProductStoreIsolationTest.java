package com.payment.service.impl;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.payment.dto.V1MerchantProductUpsertDTO;
import com.payment.entity.Product;
import com.payment.entity.Store;
import com.payment.entity.StoreProduct;
import com.payment.entity.StoreProductStock;
import com.payment.mapper.ProductChangeLogMapper;
import com.payment.mapper.ProductMapper;
import com.payment.mapper.StoreMapper;
import com.payment.mapper.StoreProductMapper;
import com.payment.mapper.StoreProductStockMapper;
import com.payment.service.StoreInventoryService;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class V1MerchantProductStoreIsolationTest {

    @BeforeAll
    static void initializeTableInfo() {
        MybatisConfiguration configuration = new MybatisConfiguration();
        MapperBuilderAssistant assistant = new MapperBuilderAssistant(configuration, "");
        TableInfoHelper.initTableInfo(assistant, Product.class);
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(configuration, ""), Store.class);
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(configuration, ""), StoreProduct.class);
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(configuration, ""), StoreProductStock.class);
    }

    @Test
    void updatingOneStoreMustNotOverwriteTenantProductPriceOrStatus() {
        ProductMapper productMapper = mock(ProductMapper.class);
        ProductChangeLogMapper changeLogMapper = mock(ProductChangeLogMapper.class);
        StoreMapper storeMapper = mock(StoreMapper.class);
        StoreProductMapper relationMapper = mock(StoreProductMapper.class);
        StoreProductStockMapper stockMapper = mock(StoreProductStockMapper.class);
        StoreInventoryService inventoryService = mock(StoreInventoryService.class);
        V1MerchantSupportService supportService = mock(V1MerchantSupportService.class);
        ProductIndexMessagePublisher indexPublisher = mock(ProductIndexMessagePublisher.class);

        Product product = new Product();
        product.setId(10L);
        product.setTenantId(9L);
        product.setProductCode("P001");
        product.setName("咖啡");
        product.setPrice(new BigDecimal("28.00"));
        product.setStatus(1);
        product.setDeleted(0);

        Store store = new Store();
        store.setId(22L);
        store.setTenantId(9L);
        store.setStatus(1);
        store.setDeleted(0);

        StoreProduct relation = new StoreProduct();
        relation.setId(100L);
        relation.setTenantId(9L);
        relation.setStoreId(22L);
        relation.setProductId(10L);
        relation.setPrice(new BigDecimal("30.00"));
        relation.setStatus(1);

        StoreProductStock stock = new StoreProductStock();
        stock.setTenantId(9L);
        stock.setStoreId(22L);
        stock.setProductId(10L);
        stock.setQuantity(5);
        stock.setLockedQuantity(0);

        when(storeMapper.selectOne(any())).thenReturn(store);
        // getTenantProduct -> product；ensureProductCodeAvailable -> null
        when(productMapper.selectOne(any())).thenReturn(product, null);
        when(relationMapper.selectOne(any())).thenReturn(relation, relation);
        when(stockMapper.selectOne(any())).thenReturn(stock);
        when(productMapper.selectById(10L)).thenReturn(product);

        V1MerchantProductServiceImpl service = new V1MerchantProductServiceImpl(
                productMapper, changeLogMapper, storeMapper, relationMapper, stockMapper,
                inventoryService, supportService, indexPublisher);

        V1MerchantProductUpsertDTO dto = new V1MerchantProductUpsertDTO();
        dto.setProductCode("P001");
        dto.setName("咖啡（B店展示名）");
        dto.setPrice(new BigDecimal("35.00"));
        dto.setUnit("杯");
        dto.setCategory("饮品");
        dto.setDescription("B店编辑商品");
        dto.setStoreId(22L);
        dto.setFulfillmentMode("STORE_PICKUP");
        dto.setStock(5);
        dto.setStatus("inactive");

        service.updateProduct(9L, 101L, 10L, dto);

        ArgumentCaptor<Product> productCaptor = ArgumentCaptor.forClass(Product.class);
        verify(productMapper).updateById(productCaptor.capture());
        assertEquals(new BigDecimal("28.00"), productCaptor.getValue().getPrice());
        assertEquals(1, productCaptor.getValue().getStatus());
        assertEquals("咖啡（B店展示名）", productCaptor.getValue().getName());

        ArgumentCaptor<StoreProduct> relationCaptor = ArgumentCaptor.forClass(StoreProduct.class);
        verify(relationMapper).updateById(relationCaptor.capture());
        assertEquals(new BigDecimal("35.00"), relationCaptor.getValue().getPrice());
        assertEquals(0, relationCaptor.getValue().getStatus());
        verifyNoInteractions(inventoryService);
    }
}
