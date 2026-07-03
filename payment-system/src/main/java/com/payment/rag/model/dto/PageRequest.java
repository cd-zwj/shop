package com.payment.rag.model.dto;

import lombok.Data;

@Data
public class PageRequest {
    private Integer page = 1;           // 当前页码，默认第1页
    private Integer pageSize = 10;      // 每页大小，默认10条
    private String userId;              // 所属用户 ID
    private String sourceType;          // 可选：按文件类型过滤 (TEXT/IMAGE/VIDEO)
    private String keyword;             // 可选：按文件名搜索
    private String sortBy = "createdAt"; // 排序字段，默认按创建时间
    private String sortOrder = "DESC";   // 排序方向，默认降序

    public Integer getOffset() {
        return (page - 1) * pageSize;
    }
}
