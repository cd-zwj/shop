package com.payment.rag.model.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.Set;

/**
 * 分片上传状态（存储在Redis）
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class ChunkUploadStatus implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 文件SHA-256哈希值
     */
    private String fileHash;

    /**
     * 文件名
     */
    private String filename;

    /**
     * 文件大小（字节）
     */
    private Long fileSize;

    /**
     * 分片总数
     */
    private Integer totalChunks;

    /**
     * 已上传的分片编号集合
     */
    private Set<Integer> uploadedChunks;

    /**
     * 上传状态
     */
    private UploadStatus status;

    /**
     * 临时目录（存放分片）
     */
    private String tempDir;

    /**
     * 创建时间戳
     */
    private Long createTime;

    /**
     * 最后更新时间戳
     */
    private Long updateTime;

    /**
     * 上传状态枚举
     */
    public enum UploadStatus {
        UPLOADING,   // 上传中
        COMPLETED,   // 已完成
        FAILED       // 失败
    }

    /**
     * 检查是否所有分片都已上传
     */
    @JsonIgnore
    public boolean isAllChunksUploaded() {
        return uploadedChunks != null && uploadedChunks.size() == totalChunks;
    }

    /**
     * 获取上传进度百分比
     */
    @JsonIgnore
    public double getProgress() {
        if (totalChunks == null || totalChunks == 0) {
            return 0.0;
        }
        return (uploadedChunks == null ? 0 : uploadedChunks.size()) * 100.0 / totalChunks;
    }
}
