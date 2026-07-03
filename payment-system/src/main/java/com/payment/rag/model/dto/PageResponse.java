package com.payment.rag.model.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PageResponse<T> {
    private List<T> records;      // 数据列表
    private Long total;           // 总记录数
    private Integer page;         // 当前页
    private Integer pageSize;     // 每页大小
    private Integer totalPages;   // 总页数

    public static <T> PageResponse<T> of(List<T> records, Long total, Integer page, Integer pageSize) {
        int totalPages = (int) Math.ceil((double) total / pageSize);
        return new PageResponse<>(records, total, page, pageSize, totalPages);
    }
}
