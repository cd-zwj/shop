package com.payment.service.impl;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.payment.common.BusinessException;
import com.payment.constant.MerchantPermission;
import com.payment.dto.V1MerchantStoreInventoryAdjustDTO;
import com.payment.entity.Product;
import com.payment.entity.Store;
import com.payment.entity.StoreInventoryChangeLog;
import com.payment.entity.StoreProductStock;
import com.payment.mapper.ProductMapper;
import com.payment.mapper.StoreInventoryChangeLogMapper;
import com.payment.mapper.StoreMapper;
import com.payment.mapper.StoreProductStockMapper;
import com.payment.service.MerchantStoreScope;
import com.payment.service.StoreInventoryService;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;
import java.util.Set;
import java.util.HashSet;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class V1MerchantStoreInventoryServiceImplTest {

    @BeforeAll
    static void initializeTableInfo() {
        MybatisConfiguration configuration = new MybatisConfiguration();
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(configuration, ""), StoreProductStock.class);
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(configuration, ""), StoreInventoryChangeLog.class);
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(configuration, ""), Store.class);
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(configuration, ""), Product.class);
    }

    @Test
    void listStocksShouldRestrictUnfilteredQueryToAssignedStores() {
        Fixture fixture = new Fixture();
        MerchantStoreScope scope = assignedScope(7L, 8L);
        when(fixture.scopeService.resolve(9L, 100L, MerchantPermission.INVENTORY_MANAGE)).thenReturn(scope);
        when(fixture.stockMapper.selectPage(any(Page.class), any(LambdaQueryWrapper.class)))
                .thenReturn(new Page<>(1, 10, 0));

        fixture.service.listStocks(9L, 100L, 1, 10, null, null, false, null);

        ArgumentCaptor<LambdaQueryWrapper<StoreProductStock>> wrapperCaptor = wrapperCaptor();
        verify(fixture.stockMapper).selectPage(any(Page.class), wrapperCaptor.capture());
        wrapperCaptor.getValue().getSqlSegment();
        assertEquals(Set.of(9L, 7L, 8L),
                new HashSet<>(wrapperCaptor.getValue().getParamNameValuePairs().values()));
    }

    @Test
    void listStocksShouldReturnEmptyPageWithoutQueryWhenAssignedScopeIsEmpty() {
        Fixture fixture = new Fixture();
        when(fixture.scopeService.resolve(9L, 100L, MerchantPermission.INVENTORY_MANAGE))
                .thenReturn(assignedScope());

        Page<?> result = fixture.service.listStocks(9L, 100L, 2, 20, null, null, false, null);

        assertEquals(0, result.getTotal());
        assertEquals(2, result.getCurrent());
        assertEquals(20, result.getSize());
        verifyNoInteractions(fixture.stockMapper);
    }

    @Test
    void listChangeLogsShouldRestrictUnfilteredQueryToAssignedStores() {
        Fixture fixture = new Fixture();
        when(fixture.scopeService.resolve(9L, 100L, MerchantPermission.INVENTORY_MANAGE))
                .thenReturn(assignedScope(7L, 8L));
        when(fixture.logMapper.selectPage(any(Page.class), any(LambdaQueryWrapper.class)))
                .thenReturn(new Page<>(1, 10, 0));

        fixture.service.listChangeLogs(9L, 100L, 1, 10, null, null);

        ArgumentCaptor<LambdaQueryWrapper<StoreInventoryChangeLog>> wrapperCaptor = wrapperCaptor();
        verify(fixture.logMapper).selectPage(any(Page.class), wrapperCaptor.capture());
        wrapperCaptor.getValue().getSqlSegment();
        assertEquals(Set.of(9L, 7L, 8L),
                new HashSet<>(wrapperCaptor.getValue().getParamNameValuePairs().values()));
    }

    @Test
    void listChangeLogsShouldReturnEmptyPageWithoutQueryWhenAssignedScopeIsEmpty() {
        Fixture fixture = new Fixture();
        when(fixture.scopeService.resolve(9L, 100L, MerchantPermission.INVENTORY_MANAGE))
                .thenReturn(assignedScope());

        Page<?> result = fixture.service.listChangeLogs(9L, 100L, 4, 15, null, null);

        assertEquals(0, result.getTotal());
        assertEquals(4, result.getCurrent());
        assertEquals(15, result.getSize());
        verifyNoInteractions(fixture.logMapper);
    }

    @Test
    void listStocksShouldRejectExplicitStoreOutsideAssignedScopeBeforeQuery() {
        Fixture fixture = new Fixture();
        MerchantStoreScope scope = assignedScope(7L);
        when(fixture.scopeService.resolve(9L, 100L, MerchantPermission.INVENTORY_MANAGE)).thenReturn(scope);
        doThrow(new BusinessException("当前员工无权访问该门店"))
                .when(fixture.scopeService).requireStoreAccess(scope, 8L);

        assertThrows(BusinessException.class,
                () -> fixture.service.listStocks(9L, 100L, 1, 10, 8L, null, false, null));

        verify(fixture.stockMapper, never()).selectPage(any(Page.class), any(LambdaQueryWrapper.class));
    }

    @Test
    void listChangeLogsShouldRejectExplicitStoreOutsideAssignedScopeBeforeQuery() {
        Fixture fixture = new Fixture();
        MerchantStoreScope scope = assignedScope(7L);
        when(fixture.scopeService.resolve(9L, 100L, MerchantPermission.INVENTORY_MANAGE)).thenReturn(scope);
        doThrow(new BusinessException("当前员工无权访问该门店"))
                .when(fixture.scopeService).requireStoreAccess(scope, 8L);

        assertThrows(BusinessException.class,
                () -> fixture.service.listChangeLogs(9L, 100L, 1, 10, 8L, null));

        verify(fixture.logMapper, never()).selectPage(any(Page.class), any(LambdaQueryWrapper.class));
    }

    @Test
    void adjustStockShouldRejectStoreOutsideAssignedScopeBeforeDownstreamCalls() {
        Fixture fixture = new Fixture();
        MerchantStoreScope scope = assignedScope(7L);
        when(fixture.scopeService.resolve(9L, 100L, MerchantPermission.INVENTORY_MANAGE)).thenReturn(scope);
        doThrow(new BusinessException("当前员工无权访问该门店"))
                .when(fixture.scopeService).requireStoreAccess(scope, 8L);
        V1MerchantStoreInventoryAdjustDTO dto = new V1MerchantStoreInventoryAdjustDTO();
        dto.setStoreId(8L);
        dto.setProductId(20L);
        dto.setDelta(3);

        assertThrows(BusinessException.class, () -> fixture.service.adjustStock(9L, 100L, dto));

        verify(fixture.storeMapper, never()).selectOne(any());
        verify(fixture.productMapper, never()).selectOne(any());
        verify(fixture.inventoryService, never()).adjust(any(), any(), any(), anyInt(), any(), any());
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static <T> ArgumentCaptor<LambdaQueryWrapper<T>> wrapperCaptor() {
        return (ArgumentCaptor) ArgumentCaptor.forClass(LambdaQueryWrapper.class);
    }

    private static MerchantStoreScope assignedScope(Long... storeIds) {
        return new MerchantStoreScope(9L, 3L, false, List.of(storeIds));
    }

    private static class Fixture {
        private final StoreProductStockMapper stockMapper = mock(StoreProductStockMapper.class);
        private final StoreInventoryChangeLogMapper logMapper = mock(StoreInventoryChangeLogMapper.class);
        private final StoreMapper storeMapper = mock(StoreMapper.class);
        private final ProductMapper productMapper = mock(ProductMapper.class);
        private final StoreInventoryService inventoryService = mock(StoreInventoryService.class);
        private final MerchantStoreScopeService scopeService = mock(MerchantStoreScopeService.class);
        private final V1MerchantStoreInventoryServiceImpl service = new V1MerchantStoreInventoryServiceImpl(
                stockMapper, logMapper, storeMapper, productMapper, inventoryService, scopeService);
    }
}
