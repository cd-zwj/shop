package com.payment.vo;

import com.payment.entity.Product;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 用户端商品视图对象，隐藏 deleted 等内部字段。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductVO {

    private Long id;
    private Long tenantId;
    private Long storeId;
    private String productCode;
    private String name;
    private Long price;
    private String unit;
    private String category;
    private String imageUrl;
    private String description;
    private Integer stock;
    private String fulfillmentMode;
    private Integer status;
    private String inventoryLabel;
    private String inventoryDescription;
    private String fulfillmentLabel;
    private String fulfillmentDescription;
    private String afterSalesNote;
    private String purchaseLimitNote;
    private String deliveryAccessDescription;
    private String deliveryAccessActionLabel;
    private Boolean purchasable;
    private String createTime;

    public static ProductVO from(Product product) {
        if (product == null) {
            return null;
        }
        ProductDetailPresentation.ProductDetailView presentation = ProductDetailPresentation.from(product);
        return ProductVO.builder()
                .id(product.getId())
                .tenantId(product.getTenantId())
                .storeId(product.getStoreId())
                .productCode(product.getProductCode())
                .name(product.getName())
                .price(VoConverterUtil.toFen(product.getPrice()))
                .unit(product.getUnit())
                .category(product.getCategory())
                .imageUrl(product.getImageUrl())
                .description(product.getDescription())
                .stock(product.getStock())
                .fulfillmentMode(product.getFulfillmentMode())
                .status(product.getStatus())
                .inventoryLabel(presentation.inventoryLabel())
                .inventoryDescription(presentation.inventoryDescription())
                .fulfillmentLabel(presentation.fulfillmentLabel())
                .fulfillmentDescription(presentation.fulfillmentDescription())
                .afterSalesNote(presentation.afterSalesNote())
                .purchaseLimitNote(presentation.purchaseLimitNote())
                .deliveryAccessDescription(presentation.deliveryAccessDescription())
                .deliveryAccessActionLabel(presentation.deliveryAccessActionLabel())
                .purchasable(presentation.purchasable())
                .createTime(VoConverterUtil.formatTime(product.getCreateTime()))
                .build();
    }
}
