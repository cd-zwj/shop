package com.payment.rag.service;

import com.payment.config.RabbitMQConfig;
import com.payment.rag.mapper.RagUnitMapper;
import com.payment.rag.model.dto.FileDeleteTask;
import com.rabbitmq.client.Channel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Service
@Slf4j
public class FileDeleteConsumer {

    private final RagUnitMapper ragUnitMapper;
    private final VectorStore leafVectorStore;
    private final VectorStore summaryVectorStore;
    private final UploadService uploadService;
    private final FileDeleteProducer fileDeleteProducer;
    private final RedisTemplate<String, Object> redisTemplate;
    private final DocumentFileService documentFileService;

    public FileDeleteConsumer(RagUnitMapper ragUnitMapper,
                              @Qualifier("leafVectorStore") VectorStore leafVectorStore,
                              @Qualifier("summaryVectorStore") VectorStore summaryVectorStore,
                              UploadService uploadService,
                              FileDeleteProducer fileDeleteProducer,
                              RedisTemplate<String, Object> redisTemplate,
                              DocumentFileService documentFileService) {
        this.ragUnitMapper = ragUnitMapper;
        this.leafVectorStore = leafVectorStore;
        this.summaryVectorStore = summaryVectorStore;
        this.uploadService = uploadService;
        this.fileDeleteProducer = fileDeleteProducer;
        this.redisTemplate = redisTemplate;
        this.documentFileService = documentFileService;
    }

    private static final String DELETE_TASK_PREFIX = "delete:task:";
    private static final int TASK_EXPIRE_HOURS = 24;

    @RabbitListener(queues = RabbitMQConfig.FILE_DELETE_QUEUE)
    public void processDelete(FileDeleteTask task,
                              Channel channel,
                              @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag) {
        try {
            if (task.getRedisStatus() != FileDeleteTask.StepStatus.SUCCESS && deleteFromRedis(task)) {
                task.setRedisStatus(FileDeleteTask.StepStatus.SUCCESS);
            }

            if (task.getMysqlStatus() != FileDeleteTask.StepStatus.SUCCESS && deleteFromMySQL(task)) {
                task.setMysqlStatus(FileDeleteTask.StepStatus.SUCCESS);
            }

            if (task.getMinioStatus() != FileDeleteTask.StepStatus.SUCCESS && deleteFromMinIO(task)) {
                task.setMinioStatus(FileDeleteTask.StepStatus.SUCCESS);
            }

            if (task.isAllSuccess()) {
                documentFileService.deleteByFileHash(task.getUserId(), task.getFileHash());
                saveTaskStatus(task);
                safeAck(channel, deliveryTag);
                return;
            }

            if (task.needsRetry()) {
                task.incrementRetry();
                if (task.getRedisStatus() != FileDeleteTask.StepStatus.SUCCESS) {
                    task.setRedisStatus(FileDeleteTask.StepStatus.PENDING);
                }
                if (task.getMysqlStatus() != FileDeleteTask.StepStatus.SUCCESS) {
                    task.setMysqlStatus(FileDeleteTask.StepStatus.PENDING);
                }
                if (task.getMinioStatus() != FileDeleteTask.StepStatus.SUCCESS) {
                    task.setMinioStatus(FileDeleteTask.StepStatus.PENDING);
                }

                saveTaskStatus(task);
                Thread.sleep(Math.min(1000L * task.getRetryCount(), 5000L));
                fileDeleteProducer.sendDeleteTask(task);
                safeAck(channel, deliveryTag);
                return;
            }

            if (task.getRedisStatus() != FileDeleteTask.StepStatus.SUCCESS) {
                task.setRedisStatus(FileDeleteTask.StepStatus.FAILED);
            }
            if (task.getMysqlStatus() != FileDeleteTask.StepStatus.SUCCESS) {
                task.setMysqlStatus(FileDeleteTask.StepStatus.FAILED);
            }
            if (task.getMinioStatus() != FileDeleteTask.StepStatus.SUCCESS) {
                task.setMinioStatus(FileDeleteTask.StepStatus.FAILED);
            }
            saveTaskStatus(task);
            safeNack(channel, deliveryTag);
        } catch (Exception e) {
            log.error("删除任务处理异常: {}", task.getFilename(), e);
            saveTaskStatus(task);
            safeNack(channel, deliveryTag);
        }
    }

    private boolean deleteFromRedis(FileDeleteTask task) {
        if (task.getVectorIds() == null || task.getVectorIds().isEmpty()) {
            return true;
        }

        boolean leafSuccess = true;
        boolean summarySuccess = true;

        try {
            leafVectorStore.delete(task.getVectorIds());
            log.info("leaf 向量删除完成, vectorIds={}", task.getVectorIds().size());
        } catch (Exception e) {
            leafSuccess = false;
            log.error("leaf 向量删除失败, vectorIds={}", task.getVectorIds().size(), e);
        }

        try {
            summaryVectorStore.delete(task.getVectorIds());
            log.info("summary 向量删除完成, vectorIds={}", task.getVectorIds().size());
        } catch (Exception e) {
            summarySuccess = false;
            log.error("summary 向量删除失败, vectorIds={}", task.getVectorIds().size(), e);
        }

        return leafSuccess && summarySuccess;
    }

    private boolean deleteFromMySQL(FileDeleteTask task) {
        try {
            if (task.getUnitIds() != null && !task.getUnitIds().isEmpty()) {
                ragUnitMapper.deleteByIds(task.getUnitIds());
            }
            return true;
        } catch (Exception e) {
            log.error("MySQL 删除失败: {}", task.getFilename(), e);
            return false;
        }
    }

    private boolean deleteFromMinIO(FileDeleteTask task) {
        try {
            if (task.getMinioPath() != null && !task.getMinioPath().isEmpty()) {
                uploadService.deleteFile(task.getMinioPath());
            }
            return true;
        } catch (Exception e) {
            log.error("MinIO 删除失败: {}", task.getFilename(), e);
            return false;
        }
    }

    private void saveTaskStatus(FileDeleteTask task) {
        try {
            redisTemplate.opsForValue().set(DELETE_TASK_PREFIX + task.getTaskId(), task, TASK_EXPIRE_HOURS, TimeUnit.HOURS);
        } catch (Exception e) {
            log.error("保存任务状态失败: {}", task.getTaskId(), e);
        }
    }

    private void safeAck(Channel channel, long deliveryTag) {
        try {
            if (channel.isOpen()) {
                channel.basicAck(deliveryTag, false);
            }
        } catch (Exception e) {
            log.error("消息确认失败: deliveryTag={}", deliveryTag, e);
        }
    }

    private void safeNack(Channel channel, long deliveryTag) {
        try {
            if (channel.isOpen()) {
                channel.basicNack(deliveryTag, false, false);
            }
            log.warn("消息 nack 成功, deliveryTag={}", deliveryTag);
        } catch (Exception e) {
            log.error("消息 nack 失败, deliveryTag={}", deliveryTag, e);
        }
    }
}
