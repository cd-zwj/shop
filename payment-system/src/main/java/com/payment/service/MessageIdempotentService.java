package com.payment.service;

/**
 * 消息幂等性服务接口
 */
public interface MessageIdempotentService {
    
    /**
     * 检查消息是否已处理
     * 
     * @param messageId 消息ID
     * @param queueName 队列名称
     * @return true-已处理，false-未处理
     */
    boolean isProcessed(String messageId, String queueName);
    
    /**
     * 记录消息处理成功
     * 
     * @param messageId 消息ID
     * @param queueName 队列名称
     * @param messageBody 消息内容
     * @param consumerName 消费者名称
     */
    void recordSuccess(String messageId, String queueName, String messageBody, String consumerName);
    
    /**
     * 记录消息处理失败
     * 
     * @param messageId 消息ID
     * @param queueName 队列名称
     * @param messageBody 消息内容
     * @param consumerName 消费者名称
     * @param errorMessage 错误信息
     */
    void recordFailure(String messageId, String queueName, String messageBody, String consumerName, String errorMessage);
}
