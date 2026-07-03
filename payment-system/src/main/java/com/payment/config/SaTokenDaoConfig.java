package com.payment.config;

import cn.dev33.satoken.dao.SaTokenDao;
import cn.dev33.satoken.dao.SaTokenDaoDefaultImpl;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

/**
 * Sa-Token storage override for local verification runs.
 *
 * The default runtime still uses the configured Sa-Token Redis integration.
 * Set app.auth.session-store=memory when a local environment needs in-memory
 * sessions, for example to isolate E2E runs from Redis driver issues.
 */
@Configuration
@ConditionalOnProperty(name = "app.auth.session-store", havingValue = "memory")
public class SaTokenDaoConfig {

    @Bean
    @Primary
    public SaTokenDao saTokenDao() {
        SaTokenDaoDefaultImpl dao = new SaTokenDaoDefaultImpl();
        dao.init();
        return dao;
    }
}
