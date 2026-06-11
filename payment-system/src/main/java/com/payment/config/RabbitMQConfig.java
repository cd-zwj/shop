package com.payment.config;

import org.springframework.amqp.core.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.Map;

/**
 * RabbitMQ配置
 */
@Configuration
public class RabbitMQConfig {
    
    // 死信交换机
    public static final String DEAD_LETTER_EXCHANGE = "payment.dlx.exchange";
    public static final String DEAD_LETTER_QUEUE = "payment.dlx.queue";
    public static final String DEAD_LETTER_ROUTING_KEY = "payment.dlx.routing";
    public static final String RECHARGE_ORDER_DELAY_QUEUE = "payment.recharge.delay";
    public static final String V1_RECHARGE_SUCCESS_QUEUE = "payment.v1.recharge.success";
    public static final String V1_ORDER_PAID_QUEUE = "payment.v1.order.paid";
    public static final String PRODUCT_INDEX_QUEUE = "payment.product.index";
    public static final String SCAN_REQUEST_QUEUE = "payment.scan.request";
    public static final String SCAN_RESULT_QUEUE = "payment.scan.result";
    
    /**
     * 充值订单延迟队列（消息过期后进入死信队列）
     */
    @Bean
    public Queue rechargeOrderDelayQueue() {
        Map<String, Object> args = new HashMap<>();
        args.put("x-dead-letter-exchange", DEAD_LETTER_EXCHANGE);
        args.put("x-dead-letter-routing-key", DEAD_LETTER_ROUTING_KEY);
        // TTL 将在发送消息时动态设置，或者这里设置默认值
        return new Queue(RECHARGE_ORDER_DELAY_QUEUE, true, false, false, args);
    }


    /**
     * 扫码请求队列
     */
    @Bean
    public Queue scanRequestQueue() {
        return new Queue(SCAN_REQUEST_QUEUE, true, false, false, dlxQueueArgs());
    }

    /**
     * 扫码处理结果队列
     */
    @Bean
    public Queue scanResultQueue() {
        return new Queue(SCAN_RESULT_QUEUE, true, false, false, dlxQueueArgs());
    }

    /**
     * v1 充值到账异步队列。
     */
    @Bean
    public Queue v1RechargeSuccessQueue() {
        return new Queue(V1_RECHARGE_SUCCESS_QUEUE, true, false, false, dlxQueueArgs());
    }

    /**
     * v1 订单支付成功异步队列。
     */
    @Bean
    public Queue v1OrderPaidQueue() {
        return new Queue(V1_ORDER_PAID_QUEUE, true, false, false, dlxQueueArgs());
    }

    /**
     * 商品索引同步队列。
     */
    @Bean
    public Queue productIndexQueue() {
        return new Queue(PRODUCT_INDEX_QUEUE, true, false, false, dlxQueueArgs());
    }
    
    /**
     * 死信交换机
     */
    @Bean
    public DirectExchange deadLetterExchange() {
        return new DirectExchange(DEAD_LETTER_EXCHANGE, true, false);
    }
    
    /**
     * 死信队列
     */
    @Bean
    public Queue deadLetterQueue() {
        return new Queue(DEAD_LETTER_QUEUE, true);
    }
    
    /**
     * 死信队列绑定到死信交换机
     */
    @Bean
    public Binding deadLetterBinding() {
        return BindingBuilder.bind(deadLetterQueue())
                .to(deadLetterExchange())
                .with(DEAD_LETTER_ROUTING_KEY);
    }

    private Map<String, Object> dlxQueueArgs() {
        Map<String, Object> args = new HashMap<>();
        args.put("x-dead-letter-exchange", DEAD_LETTER_EXCHANGE);
        args.put("x-dead-letter-routing-key", DEAD_LETTER_ROUTING_KEY);
        return args;
    }
}

