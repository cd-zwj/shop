package com.payment.config;

import org.springframework.amqp.core.Queue;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * RabbitMQ配置
 */
@Configuration
public class RabbitMQConfig {
    
    /**
     * 订单创建队列
     */
    @Bean
    public Queue orderCreatedQueue() {
        return new Queue("payment.order.created", true);
    }
    
    /**
     * 订单支付成功队列
     */
    @Bean
    public Queue orderPaidQueue() {
        return new Queue("payment.order.paid", true);
    }
    
    /**
     * 扫码请求队列
     */
    @Bean
    public Queue scanRequestQueue() {
        return new Queue("payment.scan.request", true);
    }
    
    /**
     * 扫码处理结果队列
     */
    @Bean
    public Queue scanResultQueue() {
        return new Queue("payment.scan.result", true);
    }
}

