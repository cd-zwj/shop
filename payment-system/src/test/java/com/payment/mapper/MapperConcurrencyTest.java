package com.payment.mapper;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.OptimisticLockerInnerInterceptor;
import com.baomidou.mybatisplus.extension.spring.MybatisSqlSessionFactoryBean;
import com.payment.entity.CouponTemplate;
import com.payment.entity.MemberPointsAccount;
import com.payment.entity.UserCoupon;
import org.apache.ibatis.session.SqlSessionFactory;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseBuilder;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseType;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import javax.sql.DataSource;
import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = MapperConcurrencyTest.TestConfig.class)
class MapperConcurrencyTest {

    @Configuration
    @org.mybatis.spring.annotation.MapperScan("com.payment.mapper")
    static class TestConfig {

        @Bean
        public DataSource dataSource() {
            return new EmbeddedDatabaseBuilder()
                    .setType(EmbeddedDatabaseType.H2)
                    .setName("mapper_concurrency_test;MODE=MySQL;DATABASE_TO_LOWER=TRUE")
                    .addScript("schema-test.sql")
                    .build();
        }

        @Bean
        public MybatisPlusInterceptor mybatisPlusInterceptor() {
            MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
            interceptor.addInnerInterceptor(new OptimisticLockerInnerInterceptor());
            return interceptor;
        }

        @Bean
        public SqlSessionFactory sqlSessionFactory(DataSource dataSource, MybatisPlusInterceptor interceptor) throws Exception {
            MybatisSqlSessionFactoryBean factory = new MybatisSqlSessionFactoryBean();
            factory.setDataSource(dataSource);
            factory.setPlugins(interceptor);
            MybatisConfiguration configuration = new MybatisConfiguration();
            configuration.setMapUnderscoreToCamelCase(true);
            factory.setConfiguration(configuration);
            return factory.getObject();
        }
    }

    @Autowired
    private UserCouponMapper userCouponMapper;
    @Autowired
    private CouponTemplateMapper couponTemplateMapper;
    @Autowired
    private MemberPointsAccountMapper accountMapper;

    // ──── claimCouponSlot ────

    @Test
    void claimCouponSlot_firstClaim_succeeds() {
        CouponTemplate t = buildTemplate(1, 10, 0);
        couponTemplateMapper.insert(t);
        assertEquals(1, userCouponMapper.claimCouponSlot(t.getId(), 100L), "首张领券应成功");
    }

    @Test
    void claimCouponSlot_secondClaim_sameUser_fails() {
        CouponTemplate t = buildTemplate(1, 10, 0);
        couponTemplateMapper.insert(t);
        insertUserCoupon(t.getId(), 100L);
        assertEquals(0, userCouponMapper.claimCouponSlot(t.getId(), 100L), "超出每人限领时应返回 0");
    }

    @Test
    void claimCouponSlot_stockExhausted_fails() {
        CouponTemplate t = buildTemplate(5, 1, 0);
        couponTemplateMapper.insert(t);
        assertEquals(1, userCouponMapper.claimCouponSlot(t.getId(), 100L));
        assertEquals(0, userCouponMapper.claimCouponSlot(t.getId(), 200L), "库存耗尽时应返回 0");
    }

    // ──── 积分乐观锁 ────

    @Test
    void pointsAccount_versionConflict_updateReturnsZero() {
        MemberPointsAccount a = buildAccount(500, 0);
        accountMapper.insert(a);

        accountMapper.update(null, new LambdaUpdateWrapper<MemberPointsAccount>()
                .eq(MemberPointsAccount::getId, a.getId())
                .set(MemberPointsAccount::getPoints, 200)
                .set(MemberPointsAccount::getVersion, 1));

        a.setPoints(300);
        a.setTotalUsed(200);
        assertEquals(0, accountMapper.updateById(a), "版本冲突时 updateById 应返回 0");
    }

    @Test
    void pointsAccount_versionMatch_updateReturnsOne() {
        MemberPointsAccount a = buildAccount(500, 0);
        accountMapper.insert(a);
        a.setPoints(200);
        a.setTotalUsed(300);
        assertEquals(1, accountMapper.updateById(a), "版本匹配时 updateById 应返回 1");
    }

    // ──── 辅助 ────

    private CouponTemplate buildTemplate(int perUserLimit, int totalQuantity, int receivedQuantity) {
        CouponTemplate t = new CouponTemplate();
        t.setTemplateNo("TPL_" + System.nanoTime());
        t.setTenantId(9L);
        t.setTemplateScope("TENANT");
        t.setTemplateName("测试券");
        t.setCouponType("FULL_REDUCTION");
        t.setThresholdAmount(BigDecimal.valueOf(100));
        t.setDiscountAmount(BigDecimal.valueOf(20));
        t.setTotalQuantity(totalQuantity);
        t.setReceivedQuantity(receivedQuantity);
        t.setUsedQuantity(0);
        t.setPerUserLimit(perUserLimit);
        t.setCanStackBalance(Boolean.FALSE);
        t.setCanStackPoints(Boolean.FALSE);
        t.setCanStackOtherCoupon(Boolean.FALSE);
        t.setApplicableProductScope("ALL");
        t.setValidType("FIXED_DAYS");
        t.setStatus("ACTIVE");
        t.setDeleted(0);
        t.setCreateTime(LocalDateTime.now());
        t.setUpdateTime(LocalDateTime.now());
        return t;
    }

    private void insertUserCoupon(Long templateId, Long platformUserId) {
        UserCoupon uc = new UserCoupon();
        uc.setCouponNo("UC_" + System.nanoTime());
        uc.setTemplateId(templateId);
        uc.setTenantId(9L);
        uc.setPlatformUserId(platformUserId);
        uc.setSourceType("RECEIVE");
        uc.setCouponStatus("RECEIVED");
        uc.setExpireTime(LocalDateTime.now().plusDays(7));
        uc.setVersion(0);
        uc.setCreateTime(LocalDateTime.now());
        uc.setUpdateTime(LocalDateTime.now());
        userCouponMapper.insert(uc);
    }

    private MemberPointsAccount buildAccount(int points, int totalUsed) {
        MemberPointsAccount a = new MemberPointsAccount();
        a.setTenantId(9L);
        a.setPlatformUserId(100L);
        a.setPoints(points);
        a.setTotalEarned(0);
        a.setTotalUsed(totalUsed);
        a.setVersion(0);
        a.setStatus(1);
        return a;
    }
}
