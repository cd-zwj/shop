package com.payment.rag.service;

import com.payment.config.RabbitMQConfig;
import com.payment.rag.exception.FileProcessingCancelledException;
import com.payment.rag.model.DocumentFileStatus;
import com.payment.rag.model.RagUnit;
import com.payment.rag.model.dto.FileProcessTask;
import com.payment.rag.service.processor.MediaProcessor;
import com.rabbitmq.client.Channel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;

@Service
@Slf4j
public class FileProcessConsumer {

    private final RagUnitService ragUnitService;
    private final DocumentFileService documentFileService;
    private final HierarchicalIndexingService hierarchicalIndexingService;
    private final TransactionTemplate transactionTemplate;

    public FileProcessConsumer(RagUnitService ragUnitService,
                               DocumentFileService documentFileService,
                               HierarchicalIndexingService hierarchicalIndexingService,
                               TransactionTemplate transactionTemplate) {
        this.ragUnitService = ragUnitService;
        this.documentFileService = documentFileService;
        this.hierarchicalIndexingService = hierarchicalIndexingService;
        this.transactionTemplate = transactionTemplate;
    }

    @RabbitListener(queues = RabbitMQConfig.FILE_PROCESS_QUEUE)
    public void processFile(FileProcessTask task,
                            Channel channel,
                            @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag) {
        long startTime = System.currentTimeMillis();

        try (InputStream fileStream = downloadFromMinio(task.getMinioUrl())) {
            ensureFileActive(task.getUserId(), task.getFileHash());
            if (task.getFileHash() != null) {
                documentFileService.updateStatus(task.getUserId(), task.getFileHash(), DocumentFileStatus.CHUNKING);
            }

            MediaProcessor processor = ragUnitService.findProcessorByMimeType(task.getMimeType());
            if (processor == null) {
                throw new IllegalArgumentException("不支持的文件类型: " + task.getMimeType());
            }

            List<RagUnit> leafUnits = processor.process(
                    fileStream,
                    task.getFilename(),
                    task.getMimeType(),
                    task.getMinioUrl()
            );

            // 异步主链路在这里把"叶子切片"提升为完整摘要树。
            List<RagUnit> allUnits = hierarchicalIndexingService.buildHierarchy(
                    task.getSourceId(),
                    task.getFilename(),
                    leafUnits
            );

            // 填充元数据字段（user_id / source_id 等 NOT NULL 字段必须在 saveBatch 前填充）
            for (RagUnit unit : allUnits) {
                unit.setSourceId(task.getSourceId());
                unit.setFileHash(task.getFileHash());
                unit.setUserId(task.getUserId());
                unit.setFilename(task.getFilename());
                unit.setMinioPath(task.getMinioPath());
                unit.setMinioUrl(task.getMinioUrl());
            }

            // 批量写入 MySQL（独立于事务，因为 saveBatch 使用 ExecutorType.BATCH 与 TransactionTemplate 冲突）
            if (!allUnits.isEmpty()) {
                ragUnitService.saveBatch(allUnits);
            }

            // 事务内只保留状态更新 + 向量写入
            ensureFileActive(task.getUserId(), task.getFileHash());
            saveDataWithTransaction(allUnits, task);

            int leafCount = hierarchicalIndexingService.countLeafNodes(allUnits);
            if (task.getFileHash() != null) {
                documentFileService.updateStatus(task.getUserId(), task.getFileHash(), DocumentFileStatus.SUCCESS, leafCount, null);
            }

            long duration = System.currentTimeMillis() - startTime;
            log.info("文件处理成功: {} (耗时: {}ms, 叶子切片数: {}, 总节点数: {})",
                    task.getFilename(), duration, leafCount, allUnits.size());
            safeAck(channel, deliveryTag);
        } catch (FileProcessingCancelledException e) {
            log.info("文件处理被取消: {}", task.getFilename());
            safeAck(channel, deliveryTag);
        } catch (Exception e) {
            log.error("文件处理失败: {}", task.getFilename(), e);
            cleanupPartialData(task);
            if (task.getFileHash() != null && documentFileService.isActive(task.getUserId(), task.getFileHash())) {
                documentFileService.markFailed(task.getUserId(), task.getFileHash(), normalizeErrorMessage(e));
            }
            safeNack(channel, deliveryTag);
        }
    }

    protected InputStream downloadFromMinio(String minioUrl) throws Exception {
        URL url = new URL(minioUrl);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setConnectTimeout(10000);
        connection.setReadTimeout(30000);
        InputStream rawStream = connection.getInputStream();
        // 包装为 Closeable，确保调用方关闭 InputStream 时同时断开底层连接
        return new java.io.FilterInputStream(rawStream) {
            @Override
            public void close() throws java.io.IOException {
                try {
                    super.close();
                } finally {
                    connection.disconnect();
                }
            }
        };
    }

    public void saveDataWithTransaction(List<RagUnit> units, FileProcessTask task) throws Exception {
        transactionTemplate.executeWithoutResult(status -> {
            try {
                ensureFileActive(task.getUserId(), task.getFileHash());

                int leafCount = hierarchicalIndexingService.countLeafNodes(units);

                ensureFileActive(task.getUserId(), task.getFileHash());
                if (task.getFileHash() != null) {
                    documentFileService.updateStatus(task.getUserId(), task.getFileHash(), DocumentFileStatus.VECTORIZING, leafCount, null);
                }

                if (!units.isEmpty()) {
                    ragUnitService.addUnitsToVectorStores(units, task.getFilename());
                }
            } catch (RuntimeException e) {
                status.setRollbackOnly();
                throw e;
            } catch (Exception e) {
                status.setRollbackOnly();
                throw new RuntimeException(e);
            }
        });
    }

    private void cleanupPartialData(FileProcessTask task) {
        try {
            ragUnitService.removeIndexedData(task.getSourceId());
        } catch (Exception cleanupError) {
            log.error("文件处理失败后清理残留数据失败: sourceId={}, filename={}",
                    task.getSourceId(), task.getFilename(), cleanupError);
        }
    }

    private void ensureFileActive(String userId, String fileHash) {
        if (fileHash != null && !documentFileService.isActive(userId, fileHash)) {
            throw new FileProcessingCancelledException("文件已被删除或取消处理");
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

    private String normalizeErrorMessage(Exception e) {
        if (e == null) {
            return "文件处理失败";
        }
        if (e instanceof IllegalStateException || e instanceof IllegalArgumentException) {
            return e.getMessage();
        }
        return "文件处理失败，请稍后重试";
    }
}
