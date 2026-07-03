package com.payment.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 文件资源实体，对应数据库表 file_asset。
 * <p>统一管理平台上传的文件资源，支持 MinIO、阿里云 OSS、本地存储等多种后端。
 * 记录文件的存储路径、MD5校验值、MIME类型等元数据，便于文件去重和检索。</p>
 */
@Data
@TableName("file_asset")
public class FileAsset implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 文件资源主键ID，自增 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 租户ID，用于多租户数据隔离
     */
    private Long tenantId;

    /**
     * 上传者用户ID，关联 platform_user 表
     */
    private Long platformUserId;

    /**
     * 原始文件名（用户上传时的文件名）
     */
    private String fileName;

    /**
     * 文件存储路径（如 MinIO 的 bucket + objectKey）
     */
    private String filePath;

    /**
     * 文件MD5哈希值，用于文件完整性校验和秒传去重
     */
    private String fileMd5;

    /**
     * 文件大小，单位为字节（Byte）
     */
    private Long fileSize;

    /**
     * 文件MIME类型（如 image/png、application/pdf）
     */
    private String contentType;

    /**
     * 存储后端类型：MINIO-本地MinIO / OSS-阿里云OSS / LOCAL-本地磁盘
     */
    private String storageType;

    /**
     * 文件状态：0-已删除，1-正常可用
     */
    private Integer status;

    /** 上传时间 */
    private LocalDateTime createTime;
}
