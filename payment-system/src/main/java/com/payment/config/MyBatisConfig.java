package com.payment.config;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.context.annotation.Configuration;

/**
 * MyBatis-Plus Mapper 扫描配置。
 *
 * 将 Mapper 扫描从 {@link com.payment.PaymentSystemApplication} 中拆出，
 * 避免 @WebMvcTest 切片把全部 Mapper 代理拉入测试上下文。
 */
@Configuration
@MapperScan("com.payment.mapper")
public class MyBatisConfig {
}
