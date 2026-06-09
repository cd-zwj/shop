package com.payment.service.impl;

import com.payment.common.BusinessException;
import com.payment.entity.MemberAccountTag;
import com.payment.entity.MemberLevel;
import com.payment.entity.MemberTag;
import com.payment.entity.TenantMember;
import com.payment.mapper.MemberAccountTagMapper;
import com.payment.mapper.MemberLevelMapper;
import com.payment.mapper.MemberTagMapper;
import com.payment.mapper.SalesOrderMapper;
import com.payment.mapper.TenantMemberMapper;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class MemberServiceImplTest {

    @Test
    void createLevelShouldInsertActiveTenantLevel() {
        MemberLevelMapper levelMapper = mock(MemberLevelMapper.class);
        MemberServiceImpl service = service(levelMapper, mock(MemberTagMapper.class),
                mock(MemberAccountTagMapper.class), mock(TenantMemberMapper.class));

        MemberLevel result = service.createLevel(9L, 2, "银卡", new BigDecimal("1000.00"), new BigDecimal("0.9500"));

        ArgumentCaptor<MemberLevel> captor = ArgumentCaptor.forClass(MemberLevel.class);
        verify(levelMapper).insert(captor.capture());
        assertEquals(9L, captor.getValue().getTenantId());
        assertEquals(2, captor.getValue().getLevelRank());
        assertEquals("银卡", captor.getValue().getLevelName());
        assertEquals(1000, captor.getValue().getUpgradeGrowth());
        assertEquals(new BigDecimal("0.9500"), captor.getValue().getDiscountRate());
        assertEquals(1, captor.getValue().getStatus());
        assertEquals(result, captor.getValue());
    }

    @Test
    void updateMemberLevelShouldRejectLevelOutsideTenant() {
        MemberLevelMapper levelMapper = mock(MemberLevelMapper.class);
        TenantMemberMapper memberMapper = mock(TenantMemberMapper.class);
        MemberServiceImpl service = service(levelMapper, mock(MemberTagMapper.class),
                mock(MemberAccountTagMapper.class), memberMapper);

        when(memberMapper.selectById(100L)).thenReturn(member(100L, 9L, 1));
        when(levelMapper.selectOne(any())).thenReturn(null);

        assertThrows(BusinessException.class, () -> service.updateMemberLevel(9L, 100L, 3));
        verify(memberMapper, never()).updateById(any());
    }

    @Test
    void updateMemberLevelShouldPersistNumericLevelOnTenantMember() {
        MemberLevelMapper levelMapper = mock(MemberLevelMapper.class);
        TenantMemberMapper memberMapper = mock(TenantMemberMapper.class);
        MemberServiceImpl service = service(levelMapper, mock(MemberTagMapper.class),
                mock(MemberAccountTagMapper.class), memberMapper);

        when(memberMapper.selectById(100L)).thenReturn(member(100L, 9L, 1));
        when(levelMapper.selectOne(any())).thenReturn(level(9L, 3, "金卡"));

        service.updateMemberLevel(9L, 100L, 3);

        ArgumentCaptor<TenantMember> captor = ArgumentCaptor.forClass(TenantMember.class);
        verify(memberMapper).updateById(captor.capture());
        assertEquals(3, captor.getValue().getMemberLevel());
    }

    @Test
    void assignTagShouldValidateSameTenantAndInsertWhenMissing() {
        MemberTagMapper tagMapper = mock(MemberTagMapper.class);
        TenantMemberMapper memberMapper = mock(TenantMemberMapper.class);
        MemberAccountTagMapper accountTagMapper = mock(MemberAccountTagMapper.class);
        MemberServiceImpl service = service(mock(MemberLevelMapper.class), tagMapper, accountTagMapper, memberMapper);

        when(memberMapper.selectById(100L)).thenReturn(member(100L, 9L, 1));
        when(tagMapper.selectById(18L)).thenReturn(tag(18L, 9L, "高价值"));
        when(accountTagMapper.selectOne(any())).thenReturn(null);

        service.assignTag(9L, 100L, 18L);

        ArgumentCaptor<MemberAccountTag> captor = ArgumentCaptor.forClass(MemberAccountTag.class);
        verify(accountTagMapper).insert(captor.capture());
        assertEquals(100L, captor.getValue().getPlatformUserId());
        assertEquals(18L, captor.getValue().getTagId());
    }

    @Test
    void assignTagShouldBeIdempotentWhenRelationExists() {
        MemberTagMapper tagMapper = mock(MemberTagMapper.class);
        TenantMemberMapper memberMapper = mock(TenantMemberMapper.class);
        MemberAccountTagMapper accountTagMapper = mock(MemberAccountTagMapper.class);
        MemberServiceImpl service = service(mock(MemberLevelMapper.class), tagMapper, accountTagMapper, memberMapper);

        when(memberMapper.selectById(100L)).thenReturn(member(100L, 9L, 1));
        when(tagMapper.selectById(18L)).thenReturn(tag(18L, 9L, "高价值"));
        when(accountTagMapper.selectOne(any())).thenReturn(new MemberAccountTag());

        service.assignTag(9L, 100L, 18L);

        verify(accountTagMapper, never()).insert(any());
    }

    @Test
    void assignTagShouldRejectTagOutsideTenant() {
        MemberTagMapper tagMapper = mock(MemberTagMapper.class);
        TenantMemberMapper memberMapper = mock(TenantMemberMapper.class);
        MemberAccountTagMapper accountTagMapper = mock(MemberAccountTagMapper.class);
        MemberServiceImpl service = service(mock(MemberLevelMapper.class), tagMapper, accountTagMapper, memberMapper);

        when(memberMapper.selectById(100L)).thenReturn(member(100L, 9L, 1));
        when(tagMapper.selectById(18L)).thenReturn(tag(18L, 10L, "其他商户标签"));

        assertThrows(BusinessException.class, () -> service.assignTag(9L, 100L, 18L));
        verify(accountTagMapper, never()).insert(any());
    }

    @Test
    void listTagsShouldReturnTenantTagsOrderedByCreateTime() {
        MemberTagMapper tagMapper = mock(MemberTagMapper.class);
        MemberServiceImpl service = service(mock(MemberLevelMapper.class), tagMapper,
                mock(MemberAccountTagMapper.class), mock(TenantMemberMapper.class));
        List<MemberTag> expected = List.of(tag(18L, 9L, "高价值"));
        when(tagMapper.selectList(any())).thenReturn(expected);

        assertEquals(expected, service.listTags(9L));
    }

    private MemberServiceImpl service(MemberLevelMapper levelMapper,
                                      MemberTagMapper tagMapper,
                                      MemberAccountTagMapper accountTagMapper,
                                      TenantMemberMapper memberMapper) {
        return new MemberServiceImpl(levelMapper, tagMapper, accountTagMapper, memberMapper, mock(SalesOrderMapper.class));
    }

    private MemberLevel level(Long tenantId, Integer level, String name) {
        MemberLevel entity = new MemberLevel();
        entity.setId(30L);
        entity.setTenantId(tenantId);
        entity.setLevelRank(level);
        entity.setLevelName(name);
        entity.setStatus(1);
        return entity;
    }

    private MemberTag tag(Long id, Long tenantId, String name) {
        MemberTag tag = new MemberTag();
        tag.setId(id);
        tag.setTenantId(tenantId);
        tag.setTagName(name);
        tag.setTagType("MANUAL");
        tag.setStatus(1);
        return tag;
    }

    private TenantMember member(Long id, Long tenantId, Integer level) {
        TenantMember member = new TenantMember();
        member.setId(id);
        member.setTenantId(tenantId);
        member.setPlatformUserId(200L);
        member.setMemberLevel(level);
        member.setMemberStatus(1);
        return member;
    }
}
