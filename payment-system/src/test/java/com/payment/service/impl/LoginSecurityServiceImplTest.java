package com.payment.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.payment.entity.LoginFailRecord;
import com.payment.mapper.LoginFailRecordMapper;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class LoginSecurityServiceImplTest {

    @BeforeAll
    static void initMybatisPlusCache() {
        TableInfoHelper.initTableInfo(
                new MapperBuilderAssistant(new MybatisConfiguration(), ""),
                LoginFailRecord.class
        );
    }

    @Test
    void checkNotLocked_未锁定时通过() {
        LoginFailRecordMapper mapper = mock(LoginFailRecordMapper.class);
        LoginSecurityServiceImpl service = new LoginSecurityServiceImpl(mapper);

        // 无记录
        when(mapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(null);

        assertDoesNotThrow(() -> service.checkNotLocked("user@test.com"));
    }

    @Test
    void checkNotLocked_锁定时抛异常() {
        LoginFailRecordMapper mapper = mock(LoginFailRecordMapper.class);
        LoginSecurityServiceImpl service = new LoginSecurityServiceImpl(mapper);

        LoginFailRecord record = new LoginFailRecord();
        record.setAccount("user@test.com");
        record.setFailCount(5);
        record.setLockedUntil(LocalDateTime.now().plusMinutes(20));
        when(mapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(record);

        assertThrows(RuntimeException.class, () -> service.checkNotLocked("user@test.com"));
    }

    @Test
    void recordFailure_首次失败创建记录() {
        LoginFailRecordMapper mapper = mock(LoginFailRecordMapper.class);
        LoginSecurityServiceImpl service = new LoginSecurityServiceImpl(mapper);

        // update affected=0 => 首次失败
        when(mapper.update(any(), any(LambdaUpdateWrapper.class))).thenReturn(0);
        // findByAccount 返回 null（未达锁定阈值）
        when(mapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(null);

        service.recordFailure("user@test.com", "192.168.1.1");

        ArgumentCaptor<LoginFailRecord> captor = ArgumentCaptor.forClass(LoginFailRecord.class);
        verify(mapper).insert(captor.capture());
        assertEquals("user@test.com", captor.getValue().getAccount());
        assertEquals("192.168.1.1", captor.getValue().getIp());
        assertEquals(1, captor.getValue().getFailCount());
        assertNotNull(captor.getValue().getLastFailTime());
    }

    @Test
    void recordFailure_连续失败5次触发锁定() {
        LoginFailRecordMapper mapper = mock(LoginFailRecordMapper.class);
        LoginSecurityServiceImpl service = new LoginSecurityServiceImpl(mapper);

        // 第一次 update 成功（已有记录）
        when(mapper.update(any(), any(LambdaUpdateWrapper.class))).thenReturn(1);

        // findByAccount 返回 failCount=5，尚未锁定
        LoginFailRecord record = new LoginFailRecord();
        record.setAccount("user@test.com");
        record.setFailCount(5);
        record.setLockedUntil(null);
        when(mapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(record);

        service.recordFailure("user@test.com", "192.168.1.1");

        // 验证触发了锁定的 update 调用（共两次 update：递增 + 锁定）
        verify(mapper, org.mockito.Mockito.times(2)).update(any(), any(LambdaUpdateWrapper.class));
    }

    @Test
    void recordFailure_窗口期外重置计数() {
        LoginFailRecordMapper mapper = mock(LoginFailRecordMapper.class);
        LoginSecurityServiceImpl service = new LoginSecurityServiceImpl(mapper);

        // update affected=0 => 当前窗口内无记录，走 insert
        when(mapper.update(any(), any(LambdaUpdateWrapper.class))).thenReturn(0);
        // findByAccount 返回 failCount=1，未达阈值
        LoginFailRecord record = new LoginFailRecord();
        record.setAccount("user@test.com");
        record.setFailCount(1);
        record.setLockedUntil(null);
        when(mapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(record);

        service.recordFailure("user@test.com", "10.0.0.1");

        verify(mapper).insert(any(LoginFailRecord.class));
        // 仅 1 次 update（递增），未触发锁定
        verify(mapper).update(any(), any(LambdaUpdateWrapper.class));
    }

    @Test
    void clearFailures_清除记录() {
        LoginFailRecordMapper mapper = mock(LoginFailRecordMapper.class);
        LoginSecurityServiceImpl service = new LoginSecurityServiceImpl(mapper);

        service.clearFailures("user@test.com");

        verify(mapper).delete(any(LambdaUpdateWrapper.class));
    }
}
