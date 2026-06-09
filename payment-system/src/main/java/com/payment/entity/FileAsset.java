package com.payment.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 文件媒体管理实体
 */
@Data
@TableName("file_asset")
public class FileAsset implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 租户ID
     */
    private Long tenantId;

    /**
     * 上传者用户ID
     */
    private Long platformUserId;

    /**
     * 原始文件名
     */
    private String fileName;

    /**
     * MinIO存储路径
     */
    private String filePath;

    /**
     * 文件MD5
     */
    private String fileMd5;

    /**
     * 文件大小（字节）
     */
    private Long fileSize;

    /**
     * MIME类型
     */
    private String contentType;

    /**
     * 存储类型: MINIO/OSS/LOCAL
     */
    private String storageType;

    /**
     * 状态: 0-已删除, 1-正常
     */
    private Integer status;

    private LocalDateTime createTime;
}
