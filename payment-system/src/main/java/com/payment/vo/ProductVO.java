package com.payment.vo;

import com.payment.entity.Product;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 用户端商品视图对象，隐藏 tenantId、deleted 等内部字段。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductVO {

    private Long id;
    private String productCode;
    private String name;
    private Long price;
    private String unit;
    private String category;
    private String imageUrl;
    private String description;
    private Integer status;
    private String createTime;

    public static ProductVO from(Product product) {
        if (product == null) {
            return null;
        }
        return ProductVO.builder()
                .id(product.getId())
                .productCode(product.getProductCode())
                .name(product.getName())
                .price(VoConverterUtil.toFen(product.getPrice()))
                .unit(product.getUnit())
                .category(product.getCategory())
                .imageUrl(product.getImageUrl())
                .description(product.getDescription())
                .status(product.getStatus())
                .createTime(VoConverterUtil.formatTime(product.getCreateTime()))
                .build();
    }
}
