package com.payment.rag.model;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("rag_unit")
public class RagUnit {
    /**
     * 主键ID
     */
    @TableId(type = IdType.ASSIGN_UUID)
    private String id;

    /**
     * 原始文件ID或路径
     */
    private String sourceId;              // Original file ID/Path

    /**
     * 原始文件的SHA-256哈希值（用于去重）
     */
    private String fileHash;              // SHA-256 hash of original file (for deduplication)

    private String userId;

    /**
     * 原始文件名
     */
    private String filename;              // Original filename (indexed for fast lookup)

    /**
     * 来源类型（例如：文本、文档、音视频等）
     */
    private SourceType sourceType;

    /**
     * 节点类型（例如：根节点、段落、分块等）
     */
    private RagNodeType nodeType;

    /**
     * 分块或节点的具体文本内容
     */
    private String content;

    /**
     * 标题或章节名称
     */
    private String title;

    /**
     * 文件在MinIO中的存储路径
     */
    private String minioPath;

    /**
     * 访问MinIO中文件的URL地址
     */
    private String minioUrl;

    /**
     * 此节点在父级中的分块索引
     */
    private Integer chunkIndex;

    /**
     * 父级节点ID（用于维护层级关系）
     */
    private String parentId;

    /**
     * 树形结构的层级深度（0代表根节点）
     */
    private Integer treeLevel;

    /**
     * 在同级节点中的排序序号
     */
    private Integer ordinal;

    /**
     * 子节点的总数量
     */
    private Integer childCount;

    /**
     * 音视频等流媒体片段的起始时间（毫秒）
     */
    private Long startTime;               // Video timestamp (ms)

    /**
     * 音视频等流媒体片段的结束时间（毫秒）
     */
    private Long endTime;                 // Video timestamp (ms)

    /**
     * 其他自定义扩展元数据（JSON格式）
     */
    private String metadata;              // JSON metadata

    /**
     * 记录的创建时间
     */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    /**
     * 记录的最后更新时间
     */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
