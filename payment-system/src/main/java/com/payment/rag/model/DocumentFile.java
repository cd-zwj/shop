package com.payment.rag.model;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("document_file")
public class DocumentFile {

    @TableId(type = IdType.INPUT)
    private String sourceId;

    private String fileHash;

    private String userId;

    private String filename;

    private SourceType sourceType;

    private Long fileSize;

    private String minioPath;

    private String minioUrl;

    private DocumentFileStatus status;

    private String errorMessage;

    private Integer chunkCount;

    private Boolean deleted;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
