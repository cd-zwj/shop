package com.payment.service.impl;

import com.payment.common.BusinessException;
import com.payment.entity.ProductStock;
import com.payment.mapper.ProductStockMapper;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ProductInventoryServiceImplTest {

    @Test
    void deductStockShouldRetryWhenOptimisticLockConflicts() {
        ProductStockMapper productStockMapper = mock(ProductStockMapper.class);
        ProductInventoryServiceImpl service = new ProductInventoryServiceImpl(productStockMapper);

        when(productStockMapper.selectOne(any()))
                .thenReturn(buildStock(9L, 1L, 10))
                .thenReturn(buildStock(9L, 1L, 10));
        when(productStockMapper.updateById(any()))
                .thenReturn(0)
                .thenReturn(1);

        assertDoesNotThrow(() -> service.deductStock(9L, 1L, 3, "SO001"));
        verify(productStockMapper, times(2)).updateById(any());
    }

    @Test
    void deductStockShouldFailWhenStockIsInsufficient() {
        ProductStockMapper productStockMapper = mock(ProductStockMapper.class);
        ProductInventoryServiceImpl service = new ProductInventoryServiceImpl(productStockMapper);

        when(productStockMapper.selectOne(any())).thenReturn(buildStock(9L, 1L, 1));

        assertThrows(BusinessException.class, () -> service.deductStock(9L, 1L, 3, "SO002"));
        verify(productStockMapper, times(0)).updateById(any());
    }

    private ProductStock buildStock(Long tenantId, Long productId, Integer quantity) {
        ProductStock stock = new ProductStock();
        stock.setId(1L);
        stock.setTenantId(tenantId);
        stock.setProductId(productId);
        stock.setQuantity(quantity);
        stock.setVersion(0);
        return stock;
    }
}
