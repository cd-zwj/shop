package com.payment.rag.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 文件处理任务消息体
 * 通过RabbitMQ传递，触发异步文件处理
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FileProcessTask implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 源文件ID（UUID）
     */
    private String sourceId;

    /**
     * 原始文件名
     */
    private String filename;

    /**
     * 文件SHA-256哈希值
     */
    private String fileHash;

    /**
     * 上传用户 ID
     */
    private String userId;

    /**
     * MIME类型
     */
    private String mimeType;

    /**
     * MinIO存储路径
     */
    private String minioPath;

    /**
     * MinIO访问URL
     */
    private String minioUrl;

    /**
     * 文件大小（字节）
     */
    private Long fileSize;

    /**
     * 任务创建时间戳
     */
    private Long createTimestamp;
}
