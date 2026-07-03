package com.payment.rag.service;

import com.payment.config.RabbitMQConfig;
import com.payment.rag.model.dto.FileProcessTask;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

/**
 * 文件处理消息生产者
 * 负责将文件处理任务发送到RabbitMQ队列
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class FileProcessProducer {

    private final RabbitTemplate rabbitTemplate;

    /**
     * 发送文件处理任务到队列
     *
     * @param task 文件处理任务
     */
    public void sendFileProcessTask(FileProcessTask task) {
        try {
            rabbitTemplate.convertAndSend(
                RabbitMQConfig.FILE_EXCHANGE,
                RabbitMQConfig.FILE_PROCESS_ROUTING_KEY,
                task
            );
            log.info("已发送文件处理任务到队列: sourceId={}, filename={}",
                    task.getSourceId(), task.getFilename());
        } catch (Exception e) {
            log.error("发送文件处理任务失败: {}", task.getFilename(), e);
            throw new RuntimeException("Failed to queue file process task", e);
        }
    }
}
