package com.payment.rag.service;

import com.payment.config.RabbitMQConfig;
import com.payment.rag.model.dto.FileDeleteTask;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

/**
 * 文件删除消息生产者
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class FileDeleteProducer {

    private final RabbitTemplate rabbitTemplate;

    /**
     * 发送文件删除任务到队列
     */
    public void sendDeleteTask(FileDeleteTask task) {
        try {
            rabbitTemplate.convertAndSend(
                RabbitMQConfig.FILE_EXCHANGE,
                RabbitMQConfig.FILE_DELETE_ROUTING_KEY,
                task
            );
            log.info("已发送删除任务到队列: taskId={}, filename={}, retry={}",
                    task.getTaskId(), task.getFilename(), task.getRetryCount());
        } catch (Exception e) {
            log.error("发送删除任务失败: {}", task.getFilename(), e);
            throw new RuntimeException("Failed to queue delete task", e);
        }
    }
}
