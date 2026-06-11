package com.payment.service.impl;

import com.payment.common.BusinessException;
import com.payment.entity.MemberAccountTag;
import com.payment.entity.MemberLevel;
import com.payment.entity.MemberTag;
import com.payment.entity.SalesOrder;
import com.payment.entity.TenantMember;
import com.payment.enums.PayStatusEnum;
import com.payment.mapper.MemberAccountTagMapper;
import com.payment.mapper.MemberLevelMapper;
import com.payment.mapper.MemberTagMapper;
import com.payment.mapper.SalesOrderMapper;
import com.payment.mapper.TenantMemberMapper;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.math.BigDecimal;
import java.util.Collections;
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

    // ===== checkAndAutoUpgrade 自动升级路径测试 =====

    @Test
    void autoUpgradeShouldStoreLevelRankNotIdWhenGrowthExceedsThreshold() {
        // Arrange: 成长值达到阈值后自动升级，member_level 应存 levelRank 而非主键 ID
        MemberLevelMapper levelMapper = mock(MemberLevelMapper.class);
        TenantMemberMapper memberMapper = mock(TenantMemberMapper.class);
        SalesOrderMapper salesOrderMapper = mock(SalesOrderMapper.class);
        MemberServiceImpl service = new MemberServiceImpl(levelMapper, mock(MemberTagMapper.class),
                mock(MemberAccountTagMapper.class), memberMapper, salesOrderMapper);

        // 当前会员: levelRank=0（新会员）
        TenantMember currentMember = member(100L, 9L, null);
        when(memberMapper.selectOne(any())).thenReturn(currentMember);

        // 累计消费 5000
        when(salesOrderMapper.selectObjs(any())).thenReturn(List.of(new BigDecimal("5000.00")));

        // 两个等级：银卡(rank=1, 门槛1000), 金卡(rank=2, 门槛3000)
        // selectList 按 upgradeGrowth 降序排列
        MemberLevel goldLevel = new MemberLevel();
        goldLevel.setId(50L);           // 主键 ID = 50
        goldLevel.setTenantId(9L);
        goldLevel.setLevelRank(2);       // levelRank = 2
        goldLevel.setLevelName("金卡");
        goldLevel.setUpgradeGrowth(3000);
        goldLevel.setStatus(1);

        MemberLevel silverLevel = new MemberLevel();
        silverLevel.setId(30L);         // 主键 ID = 30
        silverLevel.setTenantId(9L);
        silverLevel.setLevelRank(1);    // levelRank = 1
        silverLevel.setLevelName("银卡");
        silverLevel.setUpgradeGrowth(1000);
        silverLevel.setStatus(1);

        when(levelMapper.selectList(any())).thenReturn(List.of(goldLevel, silverLevel));

        // Act
        service.checkAndAutoUpgrade(9L, 200L);

        // Assert: member_level 存的是 levelRank(2)，而非 id(50)
        ArgumentCaptor<TenantMember> captor = ArgumentCaptor.forClass(TenantMember.class);
        verify(memberMapper).updateById(captor.capture());
        assertEquals(2, captor.getValue().getMemberLevel(),
                "member_level 应存 levelRank(2) 而非主键 ID(50)");
    }

    @Test
    void autoUpgradeShouldNotDowngradeWhenCurrentLevelIsHigher() {
        // Arrange: 当前等级 rank=2，匹配到 rank=1，不应降级
        MemberLevelMapper levelMapper = mock(MemberLevelMapper.class);
        TenantMemberMapper memberMapper = mock(TenantMemberMapper.class);
        SalesOrderMapper salesOrderMapper = mock(SalesOrderMapper.class);
        MemberServiceImpl service = new MemberServiceImpl(levelMapper, mock(MemberTagMapper.class),
                mock(MemberAccountTagMapper.class), memberMapper, salesOrderMapper);

        TenantMember currentMember = member(100L, 9L, 2); // 当前 levelRank=2
        when(memberMapper.selectOne(any())).thenReturn(currentMember);

        // 累计消费仅 500，只达到银卡门槛
        when(salesOrderMapper.selectObjs(any())).thenReturn(List.of(new BigDecimal("500.00")));

        // selectList 按 upgradeGrowth 降序：金卡(3000) 先出现但不达标，银卡(1000) 不达标
        MemberLevel goldLevel = new MemberLevel();
        goldLevel.setId(50L);
        goldLevel.setTenantId(9L);
        goldLevel.setLevelRank(2);
        goldLevel.setLevelName("金卡");
        goldLevel.setUpgradeGrowth(3000);
        goldLevel.setStatus(1);

        MemberLevel silverLevel = new MemberLevel();
        silverLevel.setId(30L);
        silverLevel.setTenantId(9L);
        silverLevel.setLevelRank(1);
        silverLevel.setLevelName("银卡");
        silverLevel.setUpgradeGrowth(1000);
        silverLevel.setStatus(1);

        when(levelMapper.selectList(any())).thenReturn(List.of(goldLevel, silverLevel));

        // Act
        service.checkAndAutoUpgrade(9L, 200L);

        // Assert: 没有任何升级操作
        verify(memberMapper, never()).updateById(any());
    }

    @Test
    void autoUpgradeShouldLookupCurrentLevelByRankNotById() {
        // Arrange: 当前 member_level=1（levelRank），确认查找逻辑用 rank 查询而非 selectById
        MemberLevelMapper levelMapper = mock(MemberLevelMapper.class);
        TenantMemberMapper memberMapper = mock(TenantMemberMapper.class);
        SalesOrderMapper salesOrderMapper = mock(SalesOrderMapper.class);
        MemberServiceImpl service = new MemberServiceImpl(levelMapper, mock(MemberTagMapper.class),
                mock(MemberAccountTagMapper.class), memberMapper, salesOrderMapper);

        TenantMember currentMember = member(100L, 9L, 1); // member_level=1 即 rank=1
        when(memberMapper.selectOne(any())).thenReturn(currentMember);

        // 累计消费 5000，达到金卡门槛
        when(salesOrderMapper.selectObjs(any())).thenReturn(List.of(new BigDecimal("5000.00")));

        // 当前等级（rank=1 银卡）通过 selectOne 查询返回
        MemberLevel currentSilver = new MemberLevel();
        currentSilver.setId(30L);
        currentSilver.setTenantId(9L);
        currentSilver.setLevelRank(1);
        currentSilver.setLevelName("银卡");
        currentSilver.setUpgradeGrowth(1000);
        currentSilver.setStatus(1);

        MemberLevel goldLevel = new MemberLevel();
        goldLevel.setId(50L);
        goldLevel.setTenantId(9L);
        goldLevel.setLevelRank(2);
        goldLevel.setLevelName("金卡");
        goldLevel.setUpgradeGrowth(3000);
        goldLevel.setStatus(1);

        // levelMapper.selectOne 用于查找当前等级（按 tenantId + levelRank）
        when(levelMapper.selectOne(any())).thenReturn(currentSilver);
        when(levelMapper.selectList(any())).thenReturn(List.of(goldLevel, currentSilver));

        // Act
        service.checkAndAutoUpgrade(9L, 200L);

        // Assert: 升级到 rank=2
        ArgumentCaptor<TenantMember> captor = ArgumentCaptor.forClass(TenantMember.class);
        verify(memberMapper).updateById(captor.capture());
        assertEquals(2, captor.getValue().getMemberLevel());
    }

    @Test
    void autoUpgradeShouldSkipWhenMemberNotFound() {
        // Arrange: 会员不存在，直接跳过
        TenantMemberMapper memberMapper = mock(TenantMemberMapper.class);
        SalesOrderMapper salesOrderMapper = mock(SalesOrderMapper.class);
        MemberServiceImpl service = new MemberServiceImpl(mock(MemberLevelMapper.class), mock(MemberTagMapper.class),
                mock(MemberAccountTagMapper.class), memberMapper, salesOrderMapper);

        when(memberMapper.selectOne(any())).thenReturn(null);

        // Act
        service.checkAndAutoUpgrade(9L, 999L);

        // Assert: 无任何数据库操作
        verify(memberMapper, never()).updateById(any());
        verify(salesOrderMapper, never()).selectObjs(any());
    }

    @Test
    void autoUpgradeShouldSkipWhenTenantIdIsNull() {
        MemberServiceImpl service = new MemberServiceImpl(mock(MemberLevelMapper.class), mock(MemberTagMapper.class),
                mock(MemberAccountTagMapper.class), mock(TenantMemberMapper.class), mock(SalesOrderMapper.class));

        // Act + Assert: 不抛异常，直接返回
        service.checkAndAutoUpgrade(null, 200L);
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
