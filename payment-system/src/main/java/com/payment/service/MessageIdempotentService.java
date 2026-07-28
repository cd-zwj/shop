package com.payment.service;

/**
 * 消息幂等性服务接口。
 * <p>
 * 保证消息消费的幂等性，防止因网络重试、消息重复投递等原因
 * 导致业务逻辑重复执行。通过记录已处理的消息 ID 实现去重。
 */
public interface MessageIdempotentService {

    /**
     * 在执行业务前原子抢占消息处理权。
     *
     * @return 抢占结果；处理中与已完成必须由消费者采用不同确认策略
     */
    MessageClaim tryClaim(String messageId, String queueName, String messageBody, String consumerName);

    /**
     * 检查消息是否已处理。
     *
     * @param messageId 消息 ID（全局唯一标识）
     * @param queueName 队列名称
     * @return true-已处理（应跳过），false-未处理（可执行）
     */
    boolean isProcessed(String messageId, String queueName);

    /**
     * 记录消息处理成功。
     * <p>
     * 消费成功后调用，将消息 ID 写入幂等记录表，防止后续重复消费。
     *
     * @param messageId    消息 ID
     * @param queueName    队列名称
     * @param messageBody  消息内容（用于审计和排查）
     * @param consumerName 消费者名称
     */
    void recordSuccess(String messageId, String queueName, String messageBody,
                       String consumerName, String claimToken);

    /**
     * 记录消息处理失败。
     *
     * @param messageId    消息 ID
     * @param queueName    队列名称
     * @param messageBody  消息内容
     * @param consumerName 消费者名称
     * @param errorMessage 错误信息
     */
    void recordFailure(String messageId, String queueName, String messageBody,
                       String consumerName, String claimToken, String errorMessage);
}
