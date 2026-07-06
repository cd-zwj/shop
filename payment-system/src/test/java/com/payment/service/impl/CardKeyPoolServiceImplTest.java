package com.payment.service.impl;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.payment.common.BusinessException;
import com.payment.constant.MerchantPermission;
import com.payment.dto.CardKeyDeliveryDTO;
import com.payment.dto.V1MerchantCardKeySummaryVO;
import com.payment.dto.V1MerchantCardKeyUploadDTO;
import com.payment.entity.CardKeyPool;
import com.payment.entity.Product;
import com.payment.enums.CardKeyStatusEnum;
import com.payment.enums.ProductTypeEnum;
import com.payment.mapper.CardKeyPoolMapper;
import com.payment.mapper.ProductMapper;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CardKeyPoolServiceImplTest {

    private CardKeyPoolMapper cardKeyPoolMapper;
    private ProductMapper productMapper;
    private V1MerchantSupportService supportService;
    private CardKeyPoolServiceImpl service;

    @BeforeAll
    static void initMybatisPlusMetadata() {
        MybatisConfiguration configuration = new MybatisConfiguration();
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(configuration, ""), CardKeyPool.class);
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(configuration, ""), Product.class);
    }

    @BeforeEach
    void setUp() {
        cardKeyPoolMapper = mock(CardKeyPoolMapper.class);
        productMapper = mock(ProductMapper.class);
        supportService = mock(V1MerchantSupportService.class);
        service = new CardKeyPoolServiceImpl(cardKeyPoolMapper, productMapper, supportService);
    }

    @Test
    void uploadShouldTrimDeduplicateAndInsertAvailableCards() {
        when(productMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(cardKeyProduct());
        when(cardKeyPoolMapper.selectList(any(LambdaQueryWrapper.class)))
                .thenReturn(List.of())
                .thenReturn(List.of(
                        cardKey(101L, CardKeyStatusEnum.AVAILABLE),
                        cardKey(102L, CardKeyStatusEnum.AVAILABLE)
                ));

        V1MerchantCardKeyUploadDTO dto = new V1MerchantCardKeyUploadDTO();
        dto.setCodes(List.of(" VIP-2026-0001 ", "", "VIP-2026-0001", "VIP-2026-0002"));

        V1MerchantCardKeySummaryVO summary = service.uploadMerchantCardKeys(1L, 100L, 10L, dto);

        verify(supportService).requirePermission(1L, 100L, MerchantPermission.PRODUCT_MANAGE);
        ArgumentCaptor<CardKeyPool> captor = ArgumentCaptor.forClass(CardKeyPool.class);
        verify(cardKeyPoolMapper, org.mockito.Mockito.times(2)).insert(captor.capture());
        assertEquals(List.of("VIP-2026-0001", "VIP-2026-0002"),
                captor.getAllValues().stream().map(CardKeyPool::getCardCode).toList());
        assertTrue(captor.getAllValues().stream()
                .allMatch(row -> CardKeyStatusEnum.AVAILABLE.name().equals(row.getStatus())));
        assertEquals(2, summary.getAvailableCount());
        assertEquals(2, summary.getTotalCount());
    }

    @Test
    void uploadShouldRejectExistingCode() {
        when(productMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(cardKeyProduct());
        when(cardKeyPoolMapper.selectList(any(LambdaQueryWrapper.class)))
                .thenReturn(List.of(cardKeyWithCode("VIP-2026-0001")));

        V1MerchantCardKeyUploadDTO dto = new V1MerchantCardKeyUploadDTO();
        dto.setCodes(List.of("VIP-2026-0001"));

        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.uploadMerchantCardKeys(1L, 100L, 10L, dto));

        assertEquals("卡密已存在: VIP-2026-0001", ex.getMessage());
        verify(cardKeyPoolMapper, never()).insert(any(CardKeyPool.class));
    }

    @Test
    void uploadShouldRejectNonCardKeyProduct() {
        Product product = cardKeyProduct();
        product.setProductType(ProductTypeEnum.PHYSICAL.name());
        when(productMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(product);

        V1MerchantCardKeyUploadDTO dto = new V1MerchantCardKeyUploadDTO();
        dto.setCodes(List.of("VIP-2026-0001"));

        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.uploadMerchantCardKeys(1L, 100L, 10L, dto));

        assertEquals("该商品不是卡密商品", ex.getMessage());
        verify(cardKeyPoolMapper, never()).insert(any(CardKeyPool.class));
    }

    @Test
    void lockForDeliveryShouldMarkAvailableCardUsed() {
        when(productMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(cardKeyProduct());
        when(cardKeyPoolMapper.selectOne(any(LambdaQueryWrapper.class)))
                .thenReturn(cardKeyWithCode(101L, "VIP-2026-0001"));
        when(cardKeyPoolMapper.update(isNull(), any(LambdaUpdateWrapper.class))).thenReturn(1);

        CardKeyDeliveryDTO result = service.lockForDelivery(1L, 10L, "O20260617001", 55L);

        assertEquals(101L, result.getCardKeyId());
        assertEquals("VIP-2026-0001", result.getCode());
        ArgumentCaptor<LambdaUpdateWrapper<CardKeyPool>> captor = ArgumentCaptor.forClass(LambdaUpdateWrapper.class);
        verify(cardKeyPoolMapper).update(isNull(), captor.capture());
        assertTrue(captor.getValue().getParamNameValuePairs().values().contains(CardKeyStatusEnum.USED.name()));
    }

    @Test
    void lockForDeliveryShouldFailWhenNoAvailableCard() {
        when(productMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(cardKeyProduct());
        when(cardKeyPoolMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(null);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.lockForDelivery(1L, 10L, "O20260617001", 55L));

        assertEquals("卡密库存不足", ex.getMessage());
        verify(cardKeyPoolMapper, never()).update(isNull(), any(LambdaUpdateWrapper.class));
    }

    @Test
    void lockForDeliveryShouldRetryAndThenFailWhenUpdateRaces() {
        when(productMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(cardKeyProduct());
        when(cardKeyPoolMapper.selectOne(any(LambdaQueryWrapper.class)))
                .thenReturn(cardKeyWithCode(101L, "VIP-2026-0001"))
                .thenReturn(cardKeyWithCode(102L, "VIP-2026-0002"))
                .thenReturn(cardKeyWithCode(103L, "VIP-2026-0003"));
        when(cardKeyPoolMapper.update(isNull(), any(LambdaUpdateWrapper.class))).thenReturn(0);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.lockForDelivery(1L, 10L, "O20260617001", 55L));

        assertEquals("卡密库存锁定失败，请重试", ex.getMessage());
        verify(cardKeyPoolMapper, org.mockito.Mockito.times(3)).update(isNull(), any(LambdaUpdateWrapper.class));
    }

    @Test
    void returnByCardKeyIdShouldMarkUsedCardReturned() {
        service.returnByCardKeyId(1L, 101L, "订单退款撤销交付");

        ArgumentCaptor<LambdaUpdateWrapper<CardKeyPool>> captor = ArgumentCaptor.forClass(LambdaUpdateWrapper.class);
        verify(cardKeyPoolMapper).update(isNull(), captor.capture());
        String params = captor.getValue().getParamNameValuePairs().values().toString();
        assertTrue(params.contains(CardKeyStatusEnum.RETURNED.name()));
        assertTrue(!params.contains(CardKeyStatusEnum.AVAILABLE.name()));
    }

    @Test
    void returnByOrderItemShouldMarkUsedCardReturned() {
        service.returnByOrderItem(1L, 55L, "订单退款撤销交付");

        ArgumentCaptor<LambdaUpdateWrapper<CardKeyPool>> captor = ArgumentCaptor.forClass(LambdaUpdateWrapper.class);
        verify(cardKeyPoolMapper).update(isNull(), captor.capture());
        String params = captor.getValue().getParamNameValuePairs().values().toString();
        assertTrue(params.contains(CardKeyStatusEnum.RETURNED.name()));
        assertTrue(!params.contains(CardKeyStatusEnum.AVAILABLE.name()));
    }

    private Product cardKeyProduct() {
        Product product = new Product();
        product.setId(10L);
        product.setTenantId(1L);
        product.setProductType(ProductTypeEnum.CARD_KEY.name());
        product.setDeleted(0);
        return product;
    }

    private CardKeyPool cardKey(Long id, CardKeyStatusEnum status) {
        CardKeyPool cardKey = new CardKeyPool();
        cardKey.setId(id);
        cardKey.setTenantId(1L);
        cardKey.setProductId(10L);
        cardKey.setCardCode("VIP-" + id);
        cardKey.setStatus(status.name());
        cardKey.setDeleted(0);
        return cardKey;
    }

    private CardKeyPool cardKeyWithCode(String code) {
        return cardKeyWithCode(101L, code);
    }

    private CardKeyPool cardKeyWithCode(Long id, String code) {
        CardKeyPool cardKey = cardKey(id, CardKeyStatusEnum.AVAILABLE);
        cardKey.setCardCode(code);
        return cardKey;
    }
}
