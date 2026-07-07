package com.payment.service.impl;

import com.payment.common.BusinessException;
import com.payment.config.RabbitMQConfig;
import com.payment.dto.AppCouponTemplateVO;
import com.payment.dto.AppUserCouponVO;
import com.payment.dto.CouponScopeCreateDTO;
import com.payment.dto.CouponTemplateCreateDTO;
import com.payment.dto.pricing.CouponDiscountCandidateDTO;
import com.payment.dto.pricing.OrderPricingItemDTO;
import com.payment.entity.CouponReceiveRecord;
import com.payment.entity.CouponExpireRecord;
import com.payment.entity.CouponScope;
import com.payment.entity.CouponTemplate;
import com.payment.entity.CouponLockRecord;
import com.payment.entity.CouponReleaseRecord;
import com.payment.entity.CouponWriteOffRecord;
import com.payment.entity.UserCoupon;
import com.payment.enums.CouponOwnerTypeEnum;
import com.payment.enums.CouponScopeTypeEnum;
import com.payment.enums.CouponTypeEnum;
import com.payment.enums.UserCouponStatusEnum;
import com.payment.mapper.CouponLockRecordMapper;
import com.payment.mapper.CouponReceiveRecordMapper;
import com.payment.mapper.CouponExpireRecordMapper;
import com.payment.mapper.CouponReleaseRecordMapper;
import com.payment.mapper.CouponScopeMapper;
import com.payment.mapper.CouponTemplateMapper;
import com.payment.mapper.CouponWriteOffRecordMapper;
import com.payment.mapper.MemberAccountTagMapper;
import com.payment.mapper.TenantMemberMapper;
import com.payment.mapper.UserCouponMapper;
import com.payment.service.OutboxPublisher;
import com.payment.service.outbox.OutboxMessageCommand;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CouponServiceImplTest {

    @Test
    void createTemplateShouldPersistDraftTenantCouponWithDefaults() {
        CouponTemplateMapper templateMapper = mock(CouponTemplateMapper.class);
        CouponServiceImpl service = service(templateMapper, mock(UserCouponMapper.class), mock(CouponReceiveRecordMapper.class),
                mock(CouponLockRecordMapper.class), mock(CouponReleaseRecordMapper.class), mock(CouponWriteOffRecordMapper.class));

        CouponTemplateCreateDTO dto = fullReductionTemplate();

        CouponTemplate result = service.createTemplate(dto);

        ArgumentCaptor<CouponTemplate> captor = ArgumentCaptor.forClass(CouponTemplate.class);
        verify(templateMapper).insert(captor.capture());
        assertEquals(9L, captor.getValue().getTenantId());
        assertEquals(CouponOwnerTypeEnum.TENANT.name(), captor.getValue().getTemplateScope());
        assertEquals(CouponTypeEnum.FULL_REDUCTION.name(), captor.getValue().getCouponType());
        assertEquals("DRAFT", captor.getValue().getStatus());
        assertEquals(0, captor.getValue().getReceivedQuantity());
        assertEquals(0, captor.getValue().getUsedQuantity());
        assertEquals(0, captor.getValue().getDeleted());
        assertNotNull(captor.getValue().getTemplateNo());
        assertEquals(result, captor.getValue());
    }

    @Test
    void listPlatformTemplatesShouldQueryOnlyPlatformCoupons() {
        CouponTemplateMapper templateMapper = mock(CouponTemplateMapper.class);
        CouponServiceImpl service = service(templateMapper, mock(UserCouponMapper.class), mock(CouponReceiveRecordMapper.class),
                mock(CouponLockRecordMapper.class), mock(CouponReleaseRecordMapper.class), mock(CouponWriteOffRecordMapper.class));
        CouponTemplate template = activeTemplate();
        template.setTenantId(null);
        template.setTemplateScope(CouponOwnerTypeEnum.PLATFORM.name());
        when(templateMapper.selectList(any())).thenReturn(List.of(template));

        List<CouponTemplate> result = service.listPlatformTemplates("ACTIVE");

        assertEquals(1, result.size());
        assertEquals(CouponOwnerTypeEnum.PLATFORM.name(), result.get(0).getTemplateScope());
    }

    @Test
    void listPlatformScopesShouldRejectMerchantCoupon() {
        CouponTemplateMapper templateMapper = mock(CouponTemplateMapper.class);
        CouponServiceImpl service = service(templateMapper, mock(CouponScopeMapper.class), mock(UserCouponMapper.class), mock(CouponReceiveRecordMapper.class),
                mock(CouponLockRecordMapper.class), mock(CouponReleaseRecordMapper.class), mock(CouponWriteOffRecordMapper.class));
        when(templateMapper.selectById(201L)).thenReturn(activeTemplate());

        assertThrows(BusinessException.class, () -> service.listPlatformScopes(201L));
    }

    @Test
    void createTemplateShouldRejectTenantCouponWithoutTenantId() {
        CouponServiceImpl service = service(mock(CouponTemplateMapper.class), mock(UserCouponMapper.class), mock(CouponReceiveRecordMapper.class),
                mock(CouponLockRecordMapper.class), mock(CouponReleaseRecordMapper.class), mock(CouponWriteOffRecordMapper.class));
        CouponTemplateCreateDTO dto = fullReductionTemplate();
        dto.setTenantId(null);

        assertThrows(BusinessException.class, () -> service.createTemplate(dto));
    }

    @Test
    void createTemplateShouldRejectInvalidDiscountRate() {
        CouponServiceImpl service = service(mock(CouponTemplateMapper.class), mock(UserCouponMapper.class), mock(CouponReceiveRecordMapper.class),
                mock(CouponLockRecordMapper.class), mock(CouponReleaseRecordMapper.class), mock(CouponWriteOffRecordMapper.class));
        CouponTemplateCreateDTO dto = fullReductionTemplate();
        dto.setCouponType(CouponTypeEnum.DISCOUNT_RATE.name());
        dto.setDiscountAmount(BigDecimal.ZERO);
        dto.setDiscountRate(new BigDecimal("1.2000"));

        assertThrows(BusinessException.class, () -> service.createTemplate(dto));
    }

    @Test
    void addScopeShouldPersistScopeAfterTemplateOwnerValidation() {
        CouponTemplateMapper templateMapper = mock(CouponTemplateMapper.class);
        CouponScopeMapper scopeMapper = mock(CouponScopeMapper.class);
        CouponServiceImpl service = service(templateMapper, scopeMapper, mock(UserCouponMapper.class), mock(CouponReceiveRecordMapper.class),
                mock(CouponLockRecordMapper.class), mock(CouponReleaseRecordMapper.class), mock(CouponWriteOffRecordMapper.class));
        when(templateMapper.selectById(201L)).thenReturn(activeTemplate());

        CouponScopeCreateDTO dto = new CouponScopeCreateDTO();
        dto.setCouponTemplateId(201L);
        dto.setTenantId(9L);
        dto.setScopeType(CouponScopeTypeEnum.PRODUCT.name());
        dto.setScopeId(7L);

        CouponScope result = service.addScope(dto);

        ArgumentCaptor<CouponScope> captor = ArgumentCaptor.forClass(CouponScope.class);
        verify(scopeMapper).insert(captor.capture());
        assertEquals(201L, captor.getValue().getCouponTemplateId());
        assertEquals(CouponScopeTypeEnum.PRODUCT.name(), captor.getValue().getScopeType());
        assertEquals(7L, captor.getValue().getScopeId());
        assertEquals(9L, captor.getValue().getTenantId());
        assertEquals(0, captor.getValue().getDeleted());
        assertEquals(result, captor.getValue());
    }

    @Test
    void addScopeShouldRejectTenantMismatchForMerchantCoupon() {
        CouponTemplateMapper templateMapper = mock(CouponTemplateMapper.class);
        CouponServiceImpl service = service(templateMapper, mock(CouponScopeMapper.class), mock(UserCouponMapper.class), mock(CouponReceiveRecordMapper.class),
                mock(CouponLockRecordMapper.class), mock(CouponReleaseRecordMapper.class), mock(CouponWriteOffRecordMapper.class));
        when(templateMapper.selectById(201L)).thenReturn(activeTemplate());

        CouponScopeCreateDTO dto = new CouponScopeCreateDTO();
        dto.setCouponTemplateId(201L);
        dto.setTenantId(10L);
        dto.setScopeType(CouponScopeTypeEnum.PRODUCT.name());
        dto.setScopeId(7L);

        assertThrows(BusinessException.class, () -> service.addScope(dto));
    }

    @Test
    void activateTemplateShouldRequireUsableValidityAndSetActive() {
        CouponTemplateMapper templateMapper = mock(CouponTemplateMapper.class);
        CouponServiceImpl service = service(templateMapper, mock(UserCouponMapper.class), mock(CouponReceiveRecordMapper.class),
                mock(CouponLockRecordMapper.class), mock(CouponReleaseRecordMapper.class), mock(CouponWriteOffRecordMapper.class));
        CouponTemplate template = activeTemplate();
        template.setStatus("DRAFT");
        when(templateMapper.selectById(201L)).thenReturn(template);

        service.activateTemplate(201L);

        ArgumentCaptor<CouponTemplate> captor = ArgumentCaptor.forClass(CouponTemplate.class);
        verify(templateMapper).updateById(captor.capture());
        assertEquals("ACTIVE", captor.getValue().getStatus());
    }

    @Test
    void disableTemplateShouldSetDisabled() {
        CouponTemplateMapper templateMapper = mock(CouponTemplateMapper.class);
        CouponServiceImpl service = service(templateMapper, mock(UserCouponMapper.class), mock(CouponReceiveRecordMapper.class),
                mock(CouponLockRecordMapper.class), mock(CouponReleaseRecordMapper.class), mock(CouponWriteOffRecordMapper.class));
        when(templateMapper.selectById(201L)).thenReturn(activeTemplate());

        service.disableTemplate(201L);

        ArgumentCaptor<CouponTemplate> captor = ArgumentCaptor.forClass(CouponTemplate.class);
        verify(templateMapper).updateById(captor.capture());
        assertEquals("DISABLED", captor.getValue().getStatus());
    }

    @Test
    void receiveCouponShouldCreateUserCouponAndReceiveRecord() {
        CouponTemplateMapper templateMapper = mock(CouponTemplateMapper.class);
        UserCouponMapper userCouponMapper = mock(UserCouponMapper.class);
        CouponReceiveRecordMapper receiveRecordMapper = mock(CouponReceiveRecordMapper.class);
        OutboxPublisher outboxPublisher = mock(OutboxPublisher.class);
        CouponServiceImpl service = service(templateMapper, userCouponMapper, receiveRecordMapper,
                mock(CouponLockRecordMapper.class), mock(CouponReleaseRecordMapper.class), mock(CouponWriteOffRecordMapper.class),
                outboxPublisher);

        when(templateMapper.selectById(201L)).thenReturn(activeTemplate());
        when(userCouponMapper.claimCouponSlot(anyLong(), anyLong())).thenReturn(1);
        when(userCouponMapper.selectCount(any())).thenReturn(0L);
        when(userCouponMapper.insert(any(UserCoupon.class))).thenAnswer(invocation -> {
            UserCoupon coupon = invocation.getArgument(0);
            coupon.setId(501L);
            return 1;
        });

        UserCoupon result = service.receiveCoupon(201L, 9L, 100L, "SO_REWARD_1");

        ArgumentCaptor<UserCoupon> couponCaptor = ArgumentCaptor.forClass(UserCoupon.class);
        verify(userCouponMapper).insert(couponCaptor.capture());
        assertEquals(201L, couponCaptor.getValue().getTemplateId());
        assertEquals(UserCouponStatusEnum.RECEIVED.name(), couponCaptor.getValue().getCouponStatus());
        assertEquals(9L, couponCaptor.getValue().getTenantId());
        assertEquals(100L, couponCaptor.getValue().getPlatformUserId());
        assertNotNull(couponCaptor.getValue().getCouponNo());
        assertNotNull(couponCaptor.getValue().getExpireTime());

        ArgumentCaptor<CouponReceiveRecord> recordCaptor = ArgumentCaptor.forClass(CouponReceiveRecord.class);
        verify(receiveRecordMapper).insert(recordCaptor.capture());
        assertEquals(201L, recordCaptor.getValue().getCouponTemplateId());
        assertEquals("SO_REWARD_1", recordCaptor.getValue().getBizNo());
        assertEquals(UserCouponStatusEnum.RECEIVED.name(), result.getCouponStatus());
        assertCouponEvent(outboxPublisher, "RECEIVED", "SO_REWARD_1", UserCouponStatusEnum.RECEIVED.name(), null);
    }

    @Test
    void receiveCouponShouldRejectPerUserLimitExceeded() {
        CouponTemplateMapper templateMapper = mock(CouponTemplateMapper.class);
        UserCouponMapper userCouponMapper = mock(UserCouponMapper.class);
        CouponServiceImpl service = service(templateMapper, userCouponMapper, mock(CouponReceiveRecordMapper.class),
                mock(CouponLockRecordMapper.class), mock(CouponReleaseRecordMapper.class), mock(CouponWriteOffRecordMapper.class));

        when(templateMapper.selectById(201L)).thenReturn(activeTemplate());
        when(userCouponMapper.selectCount(any())).thenReturn(1L);

        assertThrows(BusinessException.class, () -> service.receiveCoupon(201L, 9L, 100L, "SO_REWARD_1"));
    }

    @Test
    void receiveCouponShouldRejectWhenMemberLevelTooLow() {
        // Member level restrictions removed from entity — test now expects success
        CouponTemplateMapper templateMapper = mock(CouponTemplateMapper.class);
        UserCouponMapper userCouponMapper = mock(UserCouponMapper.class);
        CouponServiceImpl service = service(templateMapper, userCouponMapper, mock(CouponReceiveRecordMapper.class),
                mock(CouponLockRecordMapper.class), mock(CouponReleaseRecordMapper.class), mock(CouponWriteOffRecordMapper.class));

        when(templateMapper.selectById(201L)).thenReturn(activeTemplate());
        when(userCouponMapper.claimCouponSlot(anyLong(), anyLong())).thenReturn(1);
        when(userCouponMapper.selectCount(any())).thenReturn(0L);

        // No longer throws — member level fields removed from entity
        service.receiveCoupon(201L, 9L, 100L, "SO_REWARD_1");
    }

    @Test
    void lockCouponShouldMoveReceivedCouponToLockedAndWriteRecord() {
        UserCouponMapper userCouponMapper = mock(UserCouponMapper.class);
        CouponLockRecordMapper lockRecordMapper = mock(CouponLockRecordMapper.class);
        OutboxPublisher outboxPublisher = mock(OutboxPublisher.class);
        CouponServiceImpl service = service(userCouponMapper, lockRecordMapper,
                mock(CouponReleaseRecordMapper.class), mock(CouponWriteOffRecordMapper.class), outboxPublisher);

        when(userCouponMapper.selectById(501L)).thenReturn(receivedCoupon());
        when(userCouponMapper.updateById(any(UserCoupon.class))).thenReturn(1);

        service.lockCoupon(501L, 9L, 100L, 88L, "SO1001", "SO1001");

        ArgumentCaptor<UserCoupon> couponCaptor = ArgumentCaptor.forClass(UserCoupon.class);
        verify(userCouponMapper).updateById(couponCaptor.capture());
        assertEquals(UserCouponStatusEnum.LOCKED.name(), couponCaptor.getValue().getCouponStatus());
        assertEquals("SO1001", couponCaptor.getValue().getOrderNo());
        assertNotNull(couponCaptor.getValue().getLockTime());

        ArgumentCaptor<CouponLockRecord> recordCaptor = ArgumentCaptor.forClass(CouponLockRecord.class);
        verify(lockRecordMapper).insert(recordCaptor.capture());
        assertEquals(501L, recordCaptor.getValue().getUserCouponId());
        assertEquals("SO1001", recordCaptor.getValue().getBizNo());
        assertEquals(UserCouponStatusEnum.LOCKED.name(), recordCaptor.getValue().getLockStatus());
        assertCouponEvent(outboxPublisher, "LOCKED", "SO1001", UserCouponStatusEnum.LOCKED.name(), "SO1001");
    }

    @Test
    void lockCouponShouldRejectExpiredCoupon() {
        UserCouponMapper userCouponMapper = mock(UserCouponMapper.class);
        CouponServiceImpl service = service(userCouponMapper, mock(CouponLockRecordMapper.class),
                mock(CouponReleaseRecordMapper.class), mock(CouponWriteOffRecordMapper.class));

        UserCoupon coupon = receivedCoupon();
        coupon.setExpireTime(LocalDateTime.now().minusMinutes(1));
        when(userCouponMapper.selectById(501L)).thenReturn(coupon);

        assertThrows(BusinessException.class,
                () -> service.lockCoupon(501L, 9L, 100L, 88L, "SO1001", "SO1001"));
    }

    @Test
    void releaseCouponShouldRestoreReceivedStatusAndWriteReleaseRecord() {
        UserCouponMapper userCouponMapper = mock(UserCouponMapper.class);
        CouponReleaseRecordMapper releaseRecordMapper = mock(CouponReleaseRecordMapper.class);
        OutboxPublisher outboxPublisher = mock(OutboxPublisher.class);
        CouponServiceImpl service = service(userCouponMapper, mock(CouponLockRecordMapper.class),
                releaseRecordMapper, mock(CouponWriteOffRecordMapper.class), outboxPublisher);

        when(userCouponMapper.selectById(501L)).thenReturn(lockedCoupon());
        when(userCouponMapper.updateById(any(UserCoupon.class))).thenReturn(1);

        service.releaseCoupon(501L, 9L, 100L, 88L, "SO1001", "SO1001", "订单取消");

        ArgumentCaptor<UserCoupon> couponCaptor = ArgumentCaptor.forClass(UserCoupon.class);
        verify(userCouponMapper).updateById(couponCaptor.capture());
        assertEquals(UserCouponStatusEnum.RECEIVED.name(), couponCaptor.getValue().getCouponStatus());

        ArgumentCaptor<CouponReleaseRecord> recordCaptor = ArgumentCaptor.forClass(CouponReleaseRecord.class);
        verify(releaseRecordMapper).insert(recordCaptor.capture());
        assertEquals("订单取消", recordCaptor.getValue().getReleaseReason());
        assertEquals("SO1001", recordCaptor.getValue().getBizNo());
        assertCouponEvent(outboxPublisher, "RELEASED", "SO1001", UserCouponStatusEnum.RECEIVED.name(), "SO1001");
    }

    @Test
    void writeOffCouponShouldMoveLockedCouponToUsedAndWriteRecord() {
        UserCouponMapper userCouponMapper = mock(UserCouponMapper.class);
        CouponWriteOffRecordMapper writeOffRecordMapper = mock(CouponWriteOffRecordMapper.class);
        OutboxPublisher outboxPublisher = mock(OutboxPublisher.class);
        CouponServiceImpl service = service(userCouponMapper, mock(CouponLockRecordMapper.class),
                mock(CouponReleaseRecordMapper.class), writeOffRecordMapper, outboxPublisher);

        when(userCouponMapper.selectById(501L)).thenReturn(lockedCoupon());
        when(userCouponMapper.updateById(any(UserCoupon.class))).thenReturn(1);

        service.writeOffCoupon(501L, 9L, 88L, "SO1001", "SO1001", new BigDecimal("8.00"));

        ArgumentCaptor<UserCoupon> couponCaptor = ArgumentCaptor.forClass(UserCoupon.class);
        verify(userCouponMapper).updateById(couponCaptor.capture());
        assertEquals(UserCouponStatusEnum.USED.name(), couponCaptor.getValue().getCouponStatus());
        assertNotNull(couponCaptor.getValue().getUseTime());

        ArgumentCaptor<CouponWriteOffRecord> recordCaptor = ArgumentCaptor.forClass(CouponWriteOffRecord.class);
        verify(writeOffRecordMapper).insert(recordCaptor.capture());
        assertEquals(new BigDecimal("8.00"), recordCaptor.getValue().getDiscountAmount());
        assertEquals(201L, recordCaptor.getValue().getCouponTemplateId());
        assertCouponEvent(outboxPublisher, "USED", "SO1001", UserCouponStatusEnum.USED.name(), "SO1001");
    }

    @Test
    void listAvailableTemplatesShouldExposeStockAndUserLimit() {
        CouponTemplateMapper templateMapper = mock(CouponTemplateMapper.class);
        UserCouponMapper userCouponMapper = mock(UserCouponMapper.class);
        CouponServiceImpl service = service(templateMapper, userCouponMapper, mock(CouponReceiveRecordMapper.class),
                mock(CouponLockRecordMapper.class), mock(CouponReleaseRecordMapper.class), mock(CouponWriteOffRecordMapper.class));

        CouponTemplate template = activeTemplate();
        template.setTotalQuantity(10);
        template.setReceivedQuantity(4);
        template.setPerUserLimit(2);
        when(templateMapper.selectList(any())).thenReturn(List.of(template));
        when(userCouponMapper.selectCount(any())).thenReturn(1L);

        List<AppCouponTemplateVO> result = service.listAvailableTemplates(9L, 100L);

        assertEquals(1, result.size());
        assertEquals(6, result.get(0).getRemainingStock());
        assertEquals(1, result.get(0).getReceivedByCurrentUser());
        assertEquals(Boolean.TRUE, result.get(0).getReceivable());
    }

    @Test
    void listUserCouponsShouldAttachTemplateDisplayFields() {
        CouponTemplateMapper templateMapper = mock(CouponTemplateMapper.class);
        UserCouponMapper userCouponMapper = mock(UserCouponMapper.class);
        CouponServiceImpl service = service(templateMapper, userCouponMapper, mock(CouponReceiveRecordMapper.class),
                mock(CouponLockRecordMapper.class), mock(CouponReleaseRecordMapper.class), mock(CouponWriteOffRecordMapper.class));

        when(userCouponMapper.selectList(any())).thenReturn(List.of(receivedCoupon()));
        when(templateMapper.selectBatchIds(anyCollection())).thenReturn(List.of(activeTemplate()));

        List<AppUserCouponVO> result = service.listUserCoupons(9L, 100L, UserCouponStatusEnum.RECEIVED.name());

        assertEquals(1, result.size());
        assertEquals(501L, result.get(0).getId());
        assertEquals("满减券", result.get(0).getTemplateName());
        assertEquals("FULL_REDUCTION", result.get(0).getCouponType());
    }

    @Test
    void listUserCouponsShouldExposeOrderNoForUsedCouponTrace() {
        CouponTemplateMapper templateMapper = mock(CouponTemplateMapper.class);
        UserCouponMapper userCouponMapper = mock(UserCouponMapper.class);
        CouponServiceImpl service = service(templateMapper, userCouponMapper, mock(CouponReceiveRecordMapper.class),
                mock(CouponLockRecordMapper.class), mock(CouponReleaseRecordMapper.class), mock(CouponWriteOffRecordMapper.class));
        UserCoupon usedCoupon = receivedCoupon();
        usedCoupon.setCouponStatus(UserCouponStatusEnum.USED.name());
        usedCoupon.setOrderNo("SO202607060001");
        usedCoupon.setUseTime(LocalDateTime.now());

        when(userCouponMapper.selectList(any())).thenReturn(List.of(usedCoupon));
        when(templateMapper.selectBatchIds(anyCollection())).thenReturn(List.of(activeTemplate()));

        List<AppUserCouponVO> result = service.listUserCoupons(9L, 100L, UserCouponStatusEnum.USED.name());

        assertEquals(1, result.size());
        assertEquals("SO202607060001", result.get(0).getOrderNo());
        assertEquals("使用订单 SO202607060001", result.get(0).getTrace().getSource());
        assertEquals("/order/SO202607060001", result.get(0).getTrace().getActionPath());
    }

    @Test
    void resolveCouponCandidateShouldUseOnlyMatchingProductAmount() {
        CouponTemplateMapper templateMapper = mock(CouponTemplateMapper.class);
        CouponScopeMapper scopeMapper = mock(CouponScopeMapper.class);
        UserCouponMapper userCouponMapper = mock(UserCouponMapper.class);
        CouponServiceImpl service = service(templateMapper, scopeMapper, userCouponMapper, mock(CouponReceiveRecordMapper.class),
                mock(CouponLockRecordMapper.class), mock(CouponReleaseRecordMapper.class), mock(CouponWriteOffRecordMapper.class));

        CouponTemplate template = activeTemplate();
        template.setThresholdAmount(new BigDecimal("20.00"));
        template.setDiscountAmount(new BigDecimal("5.00"));
        UserCoupon coupon = receivedCoupon();
        when(userCouponMapper.selectById(501L)).thenReturn(coupon);
        when(templateMapper.selectById(201L)).thenReturn(template);
        when(scopeMapper.selectList(any())).thenReturn(List.of(productScope(201L, 7L)));

        CouponDiscountCandidateDTO result = service.resolveCouponCandidate(501L, 9L, 100L, List.of(
                pricingItem(7L, "drink", "30.00", 1),
                pricingItem(8L, "snack", "99.00", 1)
        ));

        assertEquals(new BigDecimal("30.00"), result.getEligibleAmount());
        assertEquals(new BigDecimal("5.00"), result.getDiscountAmount());
    }

    @Test
    void resolveCouponCandidateShouldRejectNonMatchingScope() {
        CouponTemplateMapper templateMapper = mock(CouponTemplateMapper.class);
        CouponScopeMapper scopeMapper = mock(CouponScopeMapper.class);
        UserCouponMapper userCouponMapper = mock(UserCouponMapper.class);
        CouponServiceImpl service = service(templateMapper, scopeMapper, userCouponMapper, mock(CouponReceiveRecordMapper.class),
                mock(CouponLockRecordMapper.class), mock(CouponReleaseRecordMapper.class), mock(CouponWriteOffRecordMapper.class));

        when(userCouponMapper.selectById(501L)).thenReturn(receivedCoupon());
        when(templateMapper.selectById(201L)).thenReturn(activeTemplate());
        when(scopeMapper.selectList(any())).thenReturn(List.of(categoryScope(201L, "coffee")));

        assertThrows(BusinessException.class, () -> service.resolveCouponCandidate(501L, 9L, 100L, List.of(
                pricingItem(7L, "drink", "30.00", 1)
        )));
    }

    @Test
    void resolveCouponCandidateShouldRejectExcludedMemberTag() {
        // Member tag restrictions removed from entity — test now expects success
        CouponTemplateMapper templateMapper = mock(CouponTemplateMapper.class);
        CouponScopeMapper scopeMapper = mock(CouponScopeMapper.class);
        UserCouponMapper userCouponMapper = mock(UserCouponMapper.class);
        CouponServiceImpl service = service(templateMapper, scopeMapper, userCouponMapper, mock(CouponReceiveRecordMapper.class),
                mock(CouponLockRecordMapper.class), mock(CouponReleaseRecordMapper.class), mock(CouponWriteOffRecordMapper.class));

        CouponTemplate template = activeTemplate();
        when(userCouponMapper.selectById(501L)).thenReturn(receivedCoupon());
        when(templateMapper.selectById(201L)).thenReturn(template);
        when(scopeMapper.selectList(any())).thenReturn(List.of());

        // No longer throws — member tag fields removed from entity
        CouponDiscountCandidateDTO result = service.resolveCouponCandidate(501L, 9L, 100L, List.of(
                pricingItem(7L, "drink", "30.00", 1)
        ));
        assertNotNull(result);
    }

    @Test
    void expireCouponsShouldMoveExpiredReceivedCouponsAndWriteRecords() {
        UserCouponMapper userCouponMapper = mock(UserCouponMapper.class);
        CouponExpireRecordMapper expireRecordMapper = mock(CouponExpireRecordMapper.class);
        OutboxPublisher outboxPublisher = mock(OutboxPublisher.class);
        CouponServiceImpl service = service(mock(CouponTemplateMapper.class), mock(CouponScopeMapper.class), userCouponMapper,
                mock(TenantMemberMapper.class), mock(MemberAccountTagMapper.class), mock(CouponReceiveRecordMapper.class),
                mock(CouponLockRecordMapper.class), mock(CouponReleaseRecordMapper.class), mock(CouponWriteOffRecordMapper.class),
                expireRecordMapper, outboxPublisher);
        UserCoupon coupon = receivedCoupon();
        coupon.setExpireTime(LocalDateTime.now().minusMinutes(10));
        when(userCouponMapper.selectList(any())).thenReturn(List.of(coupon));
        when(userCouponMapper.updateById(any(UserCoupon.class))).thenReturn(1);

        int result = service.expireCoupons(null, LocalDateTime.now(), "COUPON_EXPIRE_SCAN", "到期扫描");

        assertEquals(1, result);
        ArgumentCaptor<UserCoupon> couponCaptor = ArgumentCaptor.forClass(UserCoupon.class);
        verify(userCouponMapper).updateById(couponCaptor.capture());
        assertEquals(UserCouponStatusEnum.EXPIRED.name(), couponCaptor.getValue().getCouponStatus());

        ArgumentCaptor<CouponExpireRecord> recordCaptor = ArgumentCaptor.forClass(CouponExpireRecord.class);
        verify(expireRecordMapper).insert(recordCaptor.capture());
        assertEquals(501L, recordCaptor.getValue().getUserCouponId());
        assertEquals(201L, recordCaptor.getValue().getCouponTemplateId());
        assertEquals("COUPON_EXPIRE_SCAN", recordCaptor.getValue().getBizNo());
        assertEquals("到期扫描", recordCaptor.getValue().getExpireReason());
        assertCouponEvent(outboxPublisher, "EXPIRED", "COUPON_EXPIRE_SCAN", UserCouponStatusEnum.EXPIRED.name(), null);
    }

    private CouponServiceImpl service(UserCouponMapper userCouponMapper,
                                      CouponLockRecordMapper lockRecordMapper,
                                      CouponReleaseRecordMapper releaseRecordMapper,
                                      CouponWriteOffRecordMapper writeOffRecordMapper) {
        return service(userCouponMapper, lockRecordMapper, releaseRecordMapper, writeOffRecordMapper,
                mock(OutboxPublisher.class));
    }

    private CouponServiceImpl service(UserCouponMapper userCouponMapper,
                                      CouponLockRecordMapper lockRecordMapper,
                                      CouponReleaseRecordMapper releaseRecordMapper,
                                      CouponWriteOffRecordMapper writeOffRecordMapper,
                                      OutboxPublisher outboxPublisher) {
        return service(mock(CouponTemplateMapper.class), userCouponMapper, mock(CouponReceiveRecordMapper.class),
                lockRecordMapper, releaseRecordMapper, writeOffRecordMapper, outboxPublisher);
    }

    private CouponServiceImpl service(CouponTemplateMapper templateMapper,
                                      UserCouponMapper userCouponMapper,
                                      CouponReceiveRecordMapper receiveRecordMapper,
                                      CouponLockRecordMapper lockRecordMapper,
                                      CouponReleaseRecordMapper releaseRecordMapper,
                                      CouponWriteOffRecordMapper writeOffRecordMapper) {
        return service(templateMapper, userCouponMapper, receiveRecordMapper, lockRecordMapper, releaseRecordMapper,
                writeOffRecordMapper, mock(OutboxPublisher.class));
    }

    private CouponServiceImpl service(CouponTemplateMapper templateMapper,
                                      UserCouponMapper userCouponMapper,
                                      CouponReceiveRecordMapper receiveRecordMapper,
                                      CouponLockRecordMapper lockRecordMapper,
                                      CouponReleaseRecordMapper releaseRecordMapper,
                                      CouponWriteOffRecordMapper writeOffRecordMapper,
                                      OutboxPublisher outboxPublisher) {
        return service(templateMapper, mock(CouponScopeMapper.class), userCouponMapper, receiveRecordMapper,
                lockRecordMapper, releaseRecordMapper, writeOffRecordMapper, outboxPublisher);
    }

    private CouponServiceImpl service(CouponTemplateMapper templateMapper,
                                      CouponScopeMapper scopeMapper,
                                      UserCouponMapper userCouponMapper,
                                      CouponReceiveRecordMapper receiveRecordMapper,
                                      CouponLockRecordMapper lockRecordMapper,
                                      CouponReleaseRecordMapper releaseRecordMapper,
                                      CouponWriteOffRecordMapper writeOffRecordMapper) {
        return service(templateMapper, scopeMapper, userCouponMapper, receiveRecordMapper, lockRecordMapper,
                releaseRecordMapper, writeOffRecordMapper, mock(OutboxPublisher.class));
    }

    private CouponServiceImpl service(CouponTemplateMapper templateMapper,
                                      CouponScopeMapper scopeMapper,
                                      UserCouponMapper userCouponMapper,
                                      CouponReceiveRecordMapper receiveRecordMapper,
                                      CouponLockRecordMapper lockRecordMapper,
                                      CouponReleaseRecordMapper releaseRecordMapper,
                                      CouponWriteOffRecordMapper writeOffRecordMapper,
                                      OutboxPublisher outboxPublisher) {
        return service(templateMapper, scopeMapper, userCouponMapper, mock(TenantMemberMapper.class),
                mock(MemberAccountTagMapper.class), receiveRecordMapper, lockRecordMapper, releaseRecordMapper,
                writeOffRecordMapper, outboxPublisher);
    }

    private CouponServiceImpl service(CouponTemplateMapper templateMapper,
                                      CouponScopeMapper scopeMapper,
                                      UserCouponMapper userCouponMapper,
                                      TenantMemberMapper tenantMemberMapper,
                                      MemberAccountTagMapper memberAccountTagMapper,
                                      CouponReceiveRecordMapper receiveRecordMapper,
                                      CouponLockRecordMapper lockRecordMapper,
                                      CouponReleaseRecordMapper releaseRecordMapper,
                                      CouponWriteOffRecordMapper writeOffRecordMapper) {
        return service(templateMapper, scopeMapper, userCouponMapper, tenantMemberMapper, memberAccountTagMapper,
                receiveRecordMapper, lockRecordMapper, releaseRecordMapper, writeOffRecordMapper,
                mock(OutboxPublisher.class));
    }

    private CouponServiceImpl service(CouponTemplateMapper templateMapper,
                                      CouponScopeMapper scopeMapper,
                                      UserCouponMapper userCouponMapper,
                                      TenantMemberMapper tenantMemberMapper,
                                      MemberAccountTagMapper memberAccountTagMapper,
                                      CouponReceiveRecordMapper receiveRecordMapper,
                                      CouponLockRecordMapper lockRecordMapper,
                                      CouponReleaseRecordMapper releaseRecordMapper,
                                      CouponWriteOffRecordMapper writeOffRecordMapper,
                                      OutboxPublisher outboxPublisher) {
        return new CouponServiceImpl(
                templateMapper,
                scopeMapper,
                tenantMemberMapper,
                memberAccountTagMapper,
                userCouponMapper,
                receiveRecordMapper,
                lockRecordMapper,
                releaseRecordMapper,
                writeOffRecordMapper,
                mock(CouponExpireRecordMapper.class),
                mock(com.payment.service.UserBehaviorLogService.class),
                outboxPublisher
        );
    }

    @SuppressWarnings("unchecked")
    private void assertCouponEvent(OutboxPublisher outboxPublisher,
                                   String eventType,
                                   String bizNo,
                                   String couponStatus,
                                   String orderNo) {
        ArgumentCaptor<OutboxMessageCommand> captor = ArgumentCaptor.forClass(OutboxMessageCommand.class);
        verify(outboxPublisher).publish(captor.capture());
        OutboxMessageCommand command = captor.getValue();
        assertEquals("COUPON_EVENT", command.getBizType());
        assertEquals(bizNo, command.getBizNo());
        assertEquals(RabbitMQConfig.COUPON_EVENT_QUEUE, command.getRoutingKey());

        Map<String, Object> body = (Map<String, Object>) command.getMessageBody();
        assertEquals("COUPON_EVENT", body.get("bizType"));
        assertEquals(eventType, body.get("eventType"));
        assertEquals(bizNo, body.get("bizNo"));
        assertEquals(501L, body.get("userCouponId"));
        assertEquals(201L, body.get("couponTemplateId"));
        assertEquals(couponStatus, body.get("couponStatus"));
        assertEquals(orderNo, body.get("orderNo"));
    }

    private CouponServiceImpl service(CouponTemplateMapper templateMapper,
                                      CouponScopeMapper scopeMapper,
                                      UserCouponMapper userCouponMapper,
                                      TenantMemberMapper tenantMemberMapper,
                                      MemberAccountTagMapper memberAccountTagMapper,
                                      CouponReceiveRecordMapper receiveRecordMapper,
                                      CouponLockRecordMapper lockRecordMapper,
                                      CouponReleaseRecordMapper releaseRecordMapper,
                                      CouponWriteOffRecordMapper writeOffRecordMapper,
                                      CouponExpireRecordMapper expireRecordMapper) {
        return service(templateMapper, scopeMapper, userCouponMapper, tenantMemberMapper, memberAccountTagMapper,
                receiveRecordMapper, lockRecordMapper, releaseRecordMapper, writeOffRecordMapper, expireRecordMapper,
                mock(OutboxPublisher.class));
    }

    private CouponServiceImpl service(CouponTemplateMapper templateMapper,
                                      CouponScopeMapper scopeMapper,
                                      UserCouponMapper userCouponMapper,
                                      TenantMemberMapper tenantMemberMapper,
                                      MemberAccountTagMapper memberAccountTagMapper,
                                      CouponReceiveRecordMapper receiveRecordMapper,
                                      CouponLockRecordMapper lockRecordMapper,
                                      CouponReleaseRecordMapper releaseRecordMapper,
                                      CouponWriteOffRecordMapper writeOffRecordMapper,
                                      CouponExpireRecordMapper expireRecordMapper,
                                      OutboxPublisher outboxPublisher) {
        return new CouponServiceImpl(
                templateMapper,
                scopeMapper,
                tenantMemberMapper,
                memberAccountTagMapper,
                userCouponMapper,
                receiveRecordMapper,
                lockRecordMapper,
                releaseRecordMapper,
                writeOffRecordMapper,
                expireRecordMapper,
                mock(com.payment.service.UserBehaviorLogService.class),
                outboxPublisher
        );
    }

    private CouponTemplate activeTemplate() {
        CouponTemplate template = new CouponTemplate();
        template.setId(201L);
        template.setTenantId(9L);
        template.setTemplateScope(CouponOwnerTypeEnum.TENANT.name());
        template.setTemplateName("满减券");
        template.setCouponType("FULL_REDUCTION");
        template.setThresholdAmount(new BigDecimal("100.00"));
        template.setDiscountAmount(new BigDecimal("20.00"));
        template.setTotalQuantity(100);
        template.setReceivedQuantity(0);
        template.setPerUserLimit(1);
        template.setValidDays(7);
        template.setReceiveStartTime(LocalDateTime.now().minusDays(1));
        template.setReceiveEndTime(LocalDateTime.now().plusDays(1));
        template.setStatus("ACTIVE");
        template.setDeleted(0);
        return template;
    }

    private CouponTemplateCreateDTO fullReductionTemplate() {
        CouponTemplateCreateDTO dto = new CouponTemplateCreateDTO();
        dto.setTenantId(9L);
        dto.setTemplateScope(CouponOwnerTypeEnum.TENANT.name());
        dto.setTemplateName("满减券");
        dto.setCouponType(CouponTypeEnum.FULL_REDUCTION.name());
        dto.setThresholdAmount(new BigDecimal("100.00"));
        dto.setDiscountAmount(new BigDecimal("20.00"));
        dto.setTotalQuantity(100);
        dto.setPerUserLimit(1);
        dto.setReceiveStartTime(LocalDateTime.now().minusDays(1));
        dto.setReceiveEndTime(LocalDateTime.now().plusDays(7));
        dto.setValidDays(7);
        return dto;
    }

    private UserCoupon receivedCoupon() {
        UserCoupon coupon = new UserCoupon();
        coupon.setId(501L);
        coupon.setTemplateId(201L);
        coupon.setTenantId(9L);
        coupon.setPlatformUserId(100L);
        coupon.setSourceType("RECEIVE");
        coupon.setCouponStatus(UserCouponStatusEnum.RECEIVED.name());
        coupon.setExpireTime(LocalDateTime.now().plusDays(1));
        coupon.setVersion(0);
        return coupon;
    }

    private UserCoupon lockedCoupon() {
        UserCoupon coupon = receivedCoupon();
        coupon.setCouponStatus(UserCouponStatusEnum.LOCKED.name());
        coupon.setOrderNo("SO1001");
        coupon.setLockTime(LocalDateTime.now().minusMinutes(1));
        return coupon;
    }

    private CouponScope productScope(Long templateId, Long productId) {
        CouponScope scope = new CouponScope();
        scope.setCouponTemplateId(templateId);
        scope.setScopeType(CouponScopeTypeEnum.PRODUCT.name());
        scope.setScopeId(productId);
        scope.setDeleted(0);
        return scope;
    }

    private CouponScope categoryScope(Long templateId, String category) {
        CouponScope scope = new CouponScope();
        scope.setCouponTemplateId(templateId);
        scope.setScopeType(CouponScopeTypeEnum.CATEGORY.name());
        scope.setScopeCode(category);
        scope.setDeleted(0);
        return scope;
    }

    private OrderPricingItemDTO pricingItem(Long productId, String category, String price, Integer quantity) {
        OrderPricingItemDTO item = new OrderPricingItemDTO();
        item.setProductId(productId);
        item.setCategory(category);
        item.setUnitPrice(new BigDecimal(price));
        item.setQuantity(quantity);
        return item;
    }
}
