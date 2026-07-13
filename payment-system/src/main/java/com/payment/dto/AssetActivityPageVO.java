package com.payment.dto;

import lombok.Data;

import java.util.List;

/**
 * 资产动态分页结果。
 */
@Data
public class AssetActivityPageVO {
    private List<AppAssetActivityVO> records;
    private String nextCursor;
    private boolean hasMore;

    public AssetActivityPageVO(List<AppAssetActivityVO> records, String nextCursor, boolean hasMore) {
        this.records = records;
        this.nextCursor = nextCursor;
        this.hasMore = hasMore;
    }
}
