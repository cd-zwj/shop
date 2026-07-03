package com.payment.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.Map;

/**
 * RabbitMQ 消息队列配置。
 * <p>
 * 定义系统中所有交换机、队列及绑定关系，包括：
 * <ul>
 *   <li>业务队列：充值到账、订单支付、商品索引、优惠券事件等</li>
 *   <li>RAG 文件处理队列：文件上传/删除的异步处理</li>
 *   <li>死信队列（DLX）：消费失败的消息统一进入死信队列，由补偿任务重试</li>
 * </ul>
 * 所有业务队列均配置了死信交换机，消息消费失败后自动路由到死信队列。
 * </p>
 */
@Configuration
public class RabbitMQConfig {

    // ==================== 死信队列常量 ====================
    /** 死信交换机名称 */
    public static final String DEAD_LETTER_EXCHANGE = "payment.dlx.exchange";
    /** 死信队列名称 */
    public static final String DEAD_LETTER_QUEUE = "payment.dlx.queue";
    /** 死信路由键 */
    public static final String DEAD_LETTER_ROUTING_KEY = "payment.dlx.routing";

    // ==================== 业务队列常量 ====================
    /** 充值订单延迟队列（消息超时后进入死信队列触发补偿） */
    public static final String RECHARGE_ORDER_DELAY_QUEUE = "payment.recharge.delay";
    /** V1 充值到账异步队列 */
    public static final String V1_RECHARGE_SUCCESS_QUEUE = "payment.v1.recharge.success";
    /** V1 订单支付成功队列 */
    public static final String V1_ORDER_PAID_QUEUE = "payment.v1.order.paid";
    /** V1 订单交付队列 */
    public static final String V1_ORDER_DELIVERY_QUEUE = "payment.v1.order.delivery";
    /** 商品索引同步队列（同步到 Elasticsearch） */
    public static final String PRODUCT_INDEX_QUEUE = "payment.product.index";
    /** 优惠券状态事件队列 */
    public static final String COUPON_EVENT_QUEUE = "payment.coupon.event";
    /** 用户通知推送队列 */
    public static final String USER_NOTIFICATION_QUEUE = "payment.user.notification";
    /** 用户行为事件队列 */
    public static final String USER_BEHAVIOR_QUEUE = "payment.user.behavior";
    /** 短信发送队列 */
    public static final String SMS_SEND_QUEUE = "payment.sms.send";
    /** 积分事件队列 */
    public static final String POINTS_EVENT_QUEUE = "payment.points.event";
    /** AI 分析任务队列 */
    public static final String AI_ANALYSIS_QUEUE = "payment.ai.analysis";
    /** 扫码请求队列 */
    public static final String SCAN_REQUEST_QUEUE = "payment.scan.request";
    /** 扫码结果队列 */
    public static final String SCAN_RESULT_QUEUE = "payment.scan.result";

    // ==================== RAG 文件处理队列常量 ====================
    /** RAG 文件处理队列 */
    public static final String FILE_PROCESS_QUEUE = "rag.file.process.queue";
    /** RAG 文件处理死信队列 */
    public static final String FILE_PROCESS_DLQ = "rag.file.process.dlq";
    /** RAG 文件删除队列 */
    public static final String FILE_DELETE_QUEUE = "rag.file.delete.queue";
    /** RAG 文件删除死信队列 */
    public static final String FILE_DELETE_DLQ = "rag.file.delete.dlq";
    /** RAG 文件交换机 */
    public static final String FILE_EXCHANGE = "rag.file.exchange";
    /** RAG 文件处理路由键 */
    public static final String FILE_PROCESS_ROUTING_KEY = "file.process";
    /** RAG 文件处理死信路由键 */
    public static final String FILE_DLQ_ROUTING_KEY = "file.dlq";
    /** RAG 文件删除路由键 */
    public static final String FILE_DELETE_ROUTING_KEY = "file.delete";
    /** RAG 文件删除死信路由键 */
    public static final String FILE_DELETE_DLQ_ROUTING_KEY = "file.delete.dlq";

    /**
     * 创建 JSON 消息转换器，使用 Jackson 序列化/反序列化消息体。
     *
     * @return Jackson2JsonMessageConverter 实例
     */
    @Bean
    public MessageConverter messageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    /**
     * 创建 RabbitTemplate，配置 JSON 消息转换器。
     *
     * @param connectionFactory RabbitMQ 连接工厂
     * @param messageConverter  消息转换器
     * @return RabbitTemplate 实例
     */
    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory, MessageConverter messageConverter) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(messageConverter);
        return template;
    }

    /**
     * 创建消息监听容器工厂。
     * <p>
     * 默认使用自动 ACK，避免未显式接收 Channel 的消费者长期不确认消息。
     * 需要手动 ACK 的消费者应在 @RabbitListener 上显式声明 ackMode = "MANUAL"。
     * 同时配置预取数量 1、并发消费者 3~10，消息拒绝后不重新入队（进入死信队列）。
     *
     * @param connectionFactory RabbitMQ 连接工厂
     * @param messageConverter  消息转换器
     * @return SimpleRabbitListenerContainerFactory 实例
     */
    @Bean
    public SimpleRabbitListenerContainerFactory rabbitListenerContainerFactory(
            ConnectionFactory connectionFactory, MessageConverter messageConverter) {
        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        factory.setConnectionFactory(connectionFactory);
        factory.setMessageConverter(messageConverter);
        factory.setAcknowledgeMode(AcknowledgeMode.AUTO);
        factory.setPrefetchCount(1);
        factory.setConcurrentConsumers(3);
        factory.setMaxConcurrentConsumers(10);
        factory.setDefaultRequeueRejected(false);
        factory.setIdleEventInterval(7200000L);
        return factory;
    }

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
     * v1 订单交付队列。支付成功后由 Consumer 触发交付分发，独立于支付链路便于重试与限流。
     */
    @Bean
    public Queue v1OrderDeliveryQueue() {
        return new Queue(V1_ORDER_DELIVERY_QUEUE, true, false, false, dlxQueueArgs());
    }

    /**
     * 商品索引同步队列。
     */
    @Bean
    public Queue productIndexQueue() {
        return new Queue(PRODUCT_INDEX_QUEUE, true, false, false, dlxQueueArgs());
    }

    /**
     * 优惠券状态事件队列。
     */
    @Bean
    public Queue couponEventQueue() {
        return new Queue(COUPON_EVENT_QUEUE, true, false, false, dlxQueueArgs());
    }

    /**
     * 用户通知异步推送队列。
     */
    @Bean
    public Queue userNotificationQueue() {
        return new Queue(USER_NOTIFICATION_QUEUE, true, false, false, dlxQueueArgs());
    }

    /**
     * 用户行为事件队列，供画像与 AI 推荐链路异步消费。
     */
    @Bean
    public Queue userBehaviorQueue() {
        return new Queue(USER_BEHAVIOR_QUEUE, true, false, false, dlxQueueArgs());
    }

    /**
     * 短信发送队列。
     */
    @Bean
    public Queue smsSendQueue() {
        return new Queue(SMS_SEND_QUEUE, true, false, false, dlxQueueArgs());
    }

    /**
     * 积分事件队列。
     */
    @Bean
    public Queue pointsEventQueue() {
        return new Queue(POINTS_EVENT_QUEUE, true, false, false, dlxQueueArgs());
    }

    /**
     * AI 分析任务队列。
     */
    @Bean
    public Queue aiAnalysisQueue() {
        return new Queue(AI_ANALYSIS_QUEUE, true, false, false, dlxQueueArgs());
    }
    
    /**
     * RAG 文件处理队列。
     */
    /**
     * RAG 文件处理队列 Bean。
     *
     * @return 配置了死信路由的文件处理队列
     */
    @Bean
    public Queue fileProcessQueue() {
        Map<String, Object> args = new HashMap<>();
        args.put("x-dead-letter-exchange", FILE_EXCHANGE);
        args.put("x-dead-letter-routing-key", FILE_DLQ_ROUTING_KEY);
        return new Queue(FILE_PROCESS_QUEUE, true, false, false, args);
    }

    /**
     * RAG 文件处理死信队列 Bean。
     *
     * @return 文件处理死信队列
     */
    @Bean
    public Queue fileProcessDLQ() {
        return new Queue(FILE_PROCESS_DLQ, true);
    }

    /**
     * RAG 文件删除队列 Bean。
     *
     * @return 配置了死信路由的文件删除队列
     */
    @Bean
    public Queue fileDeleteQueue() {
        Map<String, Object> args = new HashMap<>();
        args.put("x-dead-letter-exchange", FILE_EXCHANGE);
        args.put("x-dead-letter-routing-key", FILE_DELETE_DLQ_ROUTING_KEY);
        return new Queue(FILE_DELETE_QUEUE, true, false, false, args);
    }

    /**
     * RAG 文件删除死信队列 Bean。
     *
     * @return 文件删除死信队列
     */
    @Bean
    public Queue fileDeleteDLQ() {
        return new Queue(FILE_DELETE_DLQ, true);
    }

    /**
     * RAG 文件处理交换机 Bean。
     *
     * @return 持久化 DirectExchange
     */
    @Bean
    public DirectExchange fileExchange() {
        return new DirectExchange(FILE_EXCHANGE, true, false);
    }

    /**
     * 绑定文件处理队列到文件交换机。
     *
     * @param fileProcessQueue 文件处理队列
     * @param fileExchange     文件交换机
     * @return 绑定关系
     */
    @Bean
    public Binding fileProcessBinding(Queue fileProcessQueue, DirectExchange fileExchange) {
        return BindingBuilder.bind(fileProcessQueue)
                .to(fileExchange)
                .with(FILE_PROCESS_ROUTING_KEY);
    }

    /**
     * 绑定文件处理死信队列到文件交换机。
     *
     * @param fileProcessDLQ 文件处理死信队列
     * @param fileExchange   文件交换机
     * @return 绑定关系
     */
    @Bean
    public Binding fileProcessDLQBinding(Queue fileProcessDLQ, DirectExchange fileExchange) {
        return BindingBuilder.bind(fileProcessDLQ)
                .to(fileExchange)
                .with(FILE_DLQ_ROUTING_KEY);
    }

    /**
     * 绑定文件删除队列到文件交换机。
     *
     * @param fileDeleteQueue 文件删除队列
     * @param fileExchange    文件交换机
     * @return 绑定关系
     */
    @Bean
    public Binding fileDeleteBinding(Queue fileDeleteQueue, DirectExchange fileExchange) {
        return BindingBuilder.bind(fileDeleteQueue)
                .to(fileExchange)
                .with(FILE_DELETE_ROUTING_KEY);
    }

    @Bean
    public Binding fileDeleteDLQBinding(Queue fileDeleteDLQ, DirectExchange fileExchange) {
        return BindingBuilder.bind(fileDeleteDLQ)
                .to(fileExchange)
                .with(FILE_DELETE_DLQ_ROUTING_KEY);
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

