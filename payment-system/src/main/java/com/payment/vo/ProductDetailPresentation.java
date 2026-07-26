package com.payment.vo;

import com.payment.entity.Product;

/**
 * 商品详情页用户可见说明。
 */
public final class ProductDetailPresentation {

    private ProductDetailPresentation() {
    }

    public static ProductDetailView from(Product product) {
        if (product == null) {
            return new ProductDetailView(
                    "商品信息待确认",
                    "商品详情正在同步，请稍后重试。",
                    "履约方式待确认",
                    "下单前会再次校验商品状态、库存、价格和交付方式。",
                    "售后以商家审核结果为准。",
                    "库存以结算时校验为准。",
                    "购买后可在订单详情查看履约进度。",
                    "查看订单履约",
                    false
            );
        }

        Inventory inventory = inventory(product);
        SaleStatus saleStatus = saleStatus(product);
        Fulfillment fulfillment = fulfillment(product);
        DeliveryAccess deliveryAccess = deliveryAccess(product);

        return new ProductDetailView(
                inventory.label(),
                inventory.description(),
                fulfillment.label(),
                fulfillment.description(),
                afterSalesNote(product),
                purchaseLimitNote(product),
                deliveryAccess.description(),
                deliveryAccess.actionLabel(),
                saleStatus.purchasable() && !inventory.outOfStock()
        );
    }

    private static Inventory inventory(Product product) {
        Integer stock = product.getStock();
        if (stock == null) {
            return new Inventory("库存待确认", "下单前会再次校验库存。", false);
        }
        if (stock <= 0) {
            return new Inventory("暂时缺货", "该商品当前没有可售库存。", true);
        }
        String unit = unit(product);
        String label = stock <= 5 ? "库存紧张" : "库存充足";
        return new Inventory(label, "当前可售 " + stock + " " + unit, false);
    }

    private static SaleStatus saleStatus(Product product) {
        Integer status = product.getStatus();
        boolean purchasable = status == null || status == 1;
        return new SaleStatus(purchasable);
    }

    private static Fulfillment fulfillment(Product product) {
        return new Fulfillment("到店自提", "支付成功后生成自提码，请到所选门店出示核销。");
    }

    private static String afterSalesNote(Product product) {
        return "商品可在售后期限内申请退款或退货退款，处理结果以商家审核为准。";
    }

    private static String purchaseLimitNote(Product product) {
        Integer stock = product.getStock();
        if (stock == null) {
            return "库存以结算时校验为准。";
        }
        if (stock <= 0) {
            return "当前不可购买，待商家补充库存后可下单。";
        }
        String unit = unit(product);
        return "单次立即购买 1 " + unit + "，购物车最多不超过当前库存 " + stock + " " + unit + "。";
    }

    private static DeliveryAccess deliveryAccess(Product product) {
        return new DeliveryAccess("支付完成后会生成自提凭证，请在订单详情中向门店出示。", "查看自提凭证");
    }

    private static String unit(Product product) {
        return hasText(product.getUnit()) ? product.getUnit().trim() : "件";
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private record Inventory(String label, String description, boolean outOfStock) {
    }

    private record SaleStatus(boolean purchasable) {
    }

    private record Fulfillment(String label, String description) {
    }

    private record DeliveryAccess(String description, String actionLabel) {
    }

    public record ProductDetailView(
            String inventoryLabel,
            String inventoryDescription,
            String fulfillmentLabel,
            String fulfillmentDescription,
            String afterSalesNote,
            String purchaseLimitNote,
            String deliveryAccessDescription,
            String deliveryAccessActionLabel,
            Boolean purchasable
    ) {
    }
}
