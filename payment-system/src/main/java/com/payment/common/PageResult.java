package com.payment.common;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.Data;

import java.io.Serializable;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 统一分页返回结构，对应前端期望的格式：
 * <pre>
 * {
 *   "records": [...],
 *   "total": 100,
 *   "page": 1,
 *   "size": 10
 * }
 * </pre>
 *
 * @param <T> 记录元素类型
 */
@Data
public class PageResult<T> implements Serializable {

    private static final long serialVersionUID = 1L;

    private List<T> records;
    private Long total;
    private Integer page;
    private Integer current;
    private Integer size;
    private Integer pages;

    public PageResult() {
        this.records = Collections.emptyList();
        this.total = 0L;
        this.page = 1;
        this.current = 1;
        this.size = 10;
        this.pages = 0;
    }

    public PageResult(List<T> records, long total, int page, int size) {
        this.records = records;
        this.total = total;
        this.page = page;
        this.current = page;
        this.size = size;
        this.pages = calculatePages(total, size);
    }

    /**
     * 从 MyBatis-Plus {@link Page} 转换为统一的 {@link PageResult}，并对每条记录应用映射函数。
     */
    public static <S, T> PageResult<T> from(Page<S> page, Function<S, T> converter) {
        if (page == null) {
            return new PageResult<>();
        }
        List<T> records = page.getRecords() != null
                ? page.getRecords().stream().map(converter).collect(Collectors.toList())
                : Collections.emptyList();
        return new PageResult<>(records, page.getTotal(), (int) page.getCurrent(), (int) page.getSize());
    }

    /**
     * 从 MyBatis-Plus {@link Page} 转换为统一的 {@link PageResult}。
     */
    public static <T> PageResult<T> from(Page<T> page) {
        if (page == null) {
            return new PageResult<>();
        }
        return new PageResult<>(
                page.getRecords() != null ? page.getRecords() : Collections.emptyList(),
                page.getTotal(),
                (int) page.getCurrent(),
                (int) page.getSize()
        );
    }

    private static int calculatePages(long total, int size) {
        if (size <= 0 || total <= 0) {
            return 0;
        }
        return (int) ((total + size - 1) / size);
    }
}
