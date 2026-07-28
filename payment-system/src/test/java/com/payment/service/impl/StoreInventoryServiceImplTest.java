package com.payment.service.impl;

import com.payment.common.BusinessException;
import com.payment.entity.StoreInventoryChangeLog;
import com.payment.entity.StoreProductStock;
import com.payment.mapper.StoreInventoryChangeLogMapper;
import com.payment.mapper.StoreProductStockMapper;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class StoreInventoryServiceImplTest {

    @Test
    void deductLockedShouldDecreaseStoreStockAndWriteLedger() {
        StoreProductStockMapper stockMapper = mock(StoreProductStockMapper.class);
        StoreInventoryChangeLogMapper logMapper = mock(StoreInventoryChangeLogMapper.class);
        StoreInventoryServiceImpl service = new StoreInventoryServiceImpl(stockMapper, logMapper);
        StoreProductStock stock = stock(10, 3);

        StoreInventoryChangeLog lockRecord = new StoreInventoryChangeLog();
        lockRecord.setLockedBefore(0);
        lockRecord.setLockedAfter(3);
        when(logMapper.selectBizRecordForUpdate(9L, 7L, 1L,
                "DEDUCT_LOCKED", "SALES_ORDER", "SO001")).thenReturn(null);
        when(logMapper.selectBizRecordForUpdate(9L, 7L, 1L,
                "LOCK", "SALES_ORDER", "SO001")).thenReturn(lockRecord);
        when(stockMapper.selectForUpdate(9L, 7L, 1L)).thenReturn(stock);
        when(stockMapper.updateById(any(StoreProductStock.class))).thenReturn(1);

        assertDoesNotThrow(() -> service.deductLocked(9L, 7L, 1L, 3, "SALES_ORDER", "SO001", 100L));
        assertEquals(7, stock.getQuantity());
        assertEquals(0, stock.getLockedQuantity());
        verify(logMapper).insert(any(StoreInventoryChangeLog.class));
    }

    @Test
    void deductLockedShouldRejectQuantityNotReservedForThisOrder() {
        StoreProductStockMapper stockMapper = mock(StoreProductStockMapper.class);
        StoreInventoryChangeLogMapper logMapper = mock(StoreInventoryChangeLogMapper.class);
        StoreInventoryServiceImpl service = new StoreInventoryServiceImpl(stockMapper, logMapper);

        when(stockMapper.selectForUpdate(9L, 7L, 1L)).thenReturn(stock(10, 8));

        assertThrows(BusinessException.class,
                () -> service.deductLocked(9L, 7L, 1L, 3, "SALES_ORDER", "SO002", 100L));
        verify(stockMapper, never()).updateById(any(StoreProductStock.class));
    }

    @Test
    void lockShouldBeIdempotentPerBusinessOrder() {
        StoreProductStockMapper stockMapper = mock(StoreProductStockMapper.class);
        StoreInventoryChangeLogMapper logMapper = mock(StoreInventoryChangeLogMapper.class);
        StoreInventoryServiceImpl service = new StoreInventoryServiceImpl(stockMapper, logMapper);

        StoreProductStock stock = stock(10, 3);
        when(stockMapper.selectForUpdate(9L, 7L, 1L)).thenReturn(stock);
        when(logMapper.selectBizRecordForUpdate(
                9L, 7L, 1L, "LOCK", "SALES_ORDER", "SO003"))
                .thenReturn(new StoreInventoryChangeLog());

        service.lock(9L, 7L, 1L, 3, "SALES_ORDER", "SO003");

        var ordered = inOrder(stockMapper, logMapper);
        ordered.verify(stockMapper).selectForUpdate(9L, 7L, 1L);
        ordered.verify(logMapper).selectBizRecordForUpdate(
                9L, 7L, 1L, "LOCK", "SALES_ORDER", "SO003");
        verify(stockMapper, never()).updateById(any(StoreProductStock.class));
    }

    private StoreProductStock stock(int quantity, int lockedQuantity) {
        StoreProductStock stock = new StoreProductStock();
        stock.setId(1L);
        stock.setTenantId(9L);
        stock.setStoreId(7L);
        stock.setProductId(1L);
        stock.setQuantity(quantity);
        stock.setLockedQuantity(lockedQuantity);
        stock.setVersion(0);
        return stock;
    }
}
