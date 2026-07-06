package com.payment.service.impl;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.payment.common.BusinessException;
import com.payment.constant.MerchantPermission;
import com.payment.dto.V1MerchantStoreUpsertDTO;
import com.payment.dto.V1MerchantStoreVO;
import com.payment.entity.Store;
import com.payment.mapper.StoreMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class V1MerchantStoreServiceImplTest {

    private StoreMapper storeMapper;
    private V1MerchantSupportService supportService;
    private V1MerchantStoreServiceImpl service;

    @BeforeEach
    void setUp() {
        storeMapper = mock(StoreMapper.class);
        supportService = mock(V1MerchantSupportService.class);
        service = new V1MerchantStoreServiceImpl(storeMapper, supportService);
    }

    @Test
    void listStoresShouldRequireStorePermissionAndReturnPagedVOs() {
        Store store = buildStore(10L, 1L, "ST001", "人民广场店", 1);
        Page<Store> page = new Page<>(1, 10);
        page.setRecords(List.of(store));
        page.setTotal(1);
        when(storeMapper.selectPage(any(), any())).thenReturn(page);

        Page<V1MerchantStoreVO> result = service.listStores(1L, 100L, 1, 10, "人民", 1);

        verify(supportService).requirePermission(1L, 100L, MerchantPermission.STORE_MANAGE);
        assertEquals(1L, result.getTotal());
        assertEquals("人民广场店", result.getRecords().get(0).getStoreName());
    }

    @Test
    void createStoreShouldPersistDefaults() {
        when(storeMapper.selectOne(any())).thenReturn(null);
        doAnswer(invocation -> {
            Store store = invocation.getArgument(0);
            store.setId(10L);
            return 1;
        }).when(storeMapper).insert(any(Store.class));
        when(storeMapper.selectById(10L)).thenReturn(buildStore(10L, 1L, "ST001", "人民广场店", 1));

        V1MerchantStoreUpsertDTO dto = buildUpsertDTO("ST001", "人民广场店");
        V1MerchantStoreVO vo = service.createStore(1L, 100L, dto);

        ArgumentCaptor<Store> captor = ArgumentCaptor.forClass(Store.class);
        verify(storeMapper).insert(captor.capture());
        assertEquals(1L, captor.getValue().getTenantId());
        assertEquals("DIRECT", captor.getValue().getStoreType());
        assertEquals(1, captor.getValue().getStatus());
        assertEquals(0, captor.getValue().getDeleted());
        assertEquals("人民广场店", vo.getStoreName());
    }

    @Test
    void createStoreShouldRejectDuplicateStoreNo() {
        when(storeMapper.selectOne(any())).thenReturn(buildStore(10L, 1L, "ST001", "已存在", 1));

        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.createStore(1L, 100L, buildUpsertDTO("ST001", "人民广场店")));

        assertEquals("门店编号已存在", ex.getMessage());
        verify(storeMapper, never()).insert(any(Store.class));
    }

    @Test
    void updateStoreShouldPersistFields() {
        Store existing = buildStore(10L, 1L, "ST001", "老店", 1);
        when(storeMapper.selectOne(any()))
                .thenReturn(existing)
                .thenReturn(null);
        when(storeMapper.selectById(10L)).thenReturn(buildStore(10L, 1L, "ST002", "新店", 0));

        V1MerchantStoreUpsertDTO dto = buildUpsertDTO("ST002", "新店");
        dto.setStatus(0);
        V1MerchantStoreVO vo = service.updateStore(1L, 100L, 10L, dto);

        ArgumentCaptor<Store> captor = ArgumentCaptor.forClass(Store.class);
        verify(storeMapper).updateById(captor.capture());
        assertEquals("ST002", captor.getValue().getStoreNo());
        assertEquals("新店", captor.getValue().getStoreName());
        assertEquals(0, captor.getValue().getStatus());
        assertEquals("新店", vo.getStoreName());
    }

    @Test
    void updateStoreStatusShouldRejectInvalidStatus() {
        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.updateStoreStatus(1L, 100L, 10L, 2));

        assertEquals("状态只能为0或1", ex.getMessage());
        verify(storeMapper, never()).updateById(any(Store.class));
    }

    @Test
    void deleteStoreShouldMarkDeletedAndInactive() {
        when(storeMapper.selectOne(any())).thenReturn(buildStore(10L, 1L, "ST001", "人民广场店", 1));

        service.deleteStore(1L, 100L, 10L);

        ArgumentCaptor<Store> captor = ArgumentCaptor.forClass(Store.class);
        verify(storeMapper).updateById(captor.capture());
        assertEquals(1, captor.getValue().getDeleted());
        assertEquals(0, captor.getValue().getStatus());
    }

    private V1MerchantStoreUpsertDTO buildUpsertDTO(String storeNo, String storeName) {
        V1MerchantStoreUpsertDTO dto = new V1MerchantStoreUpsertDTO();
        dto.setStoreNo(storeNo);
        dto.setStoreName(storeName);
        return dto;
    }

    private Store buildStore(Long id, Long tenantId, String storeNo, String storeName, Integer status) {
        Store store = new Store();
        store.setId(id);
        store.setTenantId(tenantId);
        store.setStoreNo(storeNo);
        store.setStoreName(storeName);
        store.setStoreType("DIRECT");
        store.setStatus(status);
        store.setDeleted(0);
        return store;
    }
}
