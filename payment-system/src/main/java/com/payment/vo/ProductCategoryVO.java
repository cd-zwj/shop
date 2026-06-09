package com.payment.vo;

import com.payment.entity.ProductCategory;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Collections;
import java.util.List;

/**
 * 商品分类视图对象，排除 deleted 等内部字段，支持树形 children 列表。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductCategoryVO {

    private Long id;
    private Long tenantId;
    private String name;
    private Long parentId;
    private Integer sortOrder;
    private String icon;
    private Integer status;
    private String createTime;
    private String updateTime;

    @Builder.Default
    private List<ProductCategoryVO> children = Collections.emptyList();

    public static ProductCategoryVO from(ProductCategory category) {
        if (category == null) {
            return null;
        }
        return ProductCategoryVO.builder()
                .id(category.getId())
                .tenantId(category.getTenantId())
                .name(category.getName())
                .parentId(category.getParentId())
                .sortOrder(category.getSortOrder())
                .icon(category.getIcon())
                .status(category.getStatus())
                .createTime(VoConverterUtil.formatTime(category.getCreateTime()))
                .updateTime(VoConverterUtil.formatTime(category.getUpdateTime()))
                .children(Collections.emptyList())
                .build();
    }
}
