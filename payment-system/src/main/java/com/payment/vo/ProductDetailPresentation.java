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
        String mode = product.getFulfillmentMode();
        String type = product.getProductType();
        if ("ONLINE_VIRTUAL".equals(mode)) {
            if ("CARD_KEY".equals(type)) {
                return new Fulfillment("卡密自动交付", "支付成功后，系统会自动发放可用卡密。");
            }
            if ("SUBSCRIPTION".equals(type)) {
                return new Fulfillment("订阅权益", "支付成功后订阅权益会自动激活，并生成线上交付记录。");
            }
            return new Fulfillment("线上交付", "支付成功后，系统会生成线上交付记录。");
        }
        if ("OFFLINE_SERVICE".equals(mode)) {
            return new Fulfillment("线下服务", "支付成功后生成服务凭证，到店或按商家约定核销。");
        }
        return new Fulfillment("快递发货", "支付后由商家按订单信息安排发货。");
    }

    private static String afterSalesNote(Product product) {
        String mode = product.getFulfillmentMode();
        String type = product.getProductType();
        if ("ONLINE_VIRTUAL".equals(mode)) {
            return "CARD_KEY".equals(type)
                    ? "卡密未使用前可提交售后申请，已使用内容需由商家审核。"
                    : "虚拟内容交付后仍可提交售后申请，处理结果以商家审核为准。";
        }
        if ("OFFLINE_SERVICE".equals(mode)) {
            return "服务未核销前可申请售后，已核销订单需商家审核。";
        }
        return "实物商品按订单售后流程处理，退款或退货退款由商家审核。";
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
        String mode = product.getFulfillmentMode();
        String type = product.getProductType();
        if ("ONLINE_VIRTUAL".equals(mode)) {
            if ("CARD_KEY".equals(type)) {
                return new DeliveryAccess("支付完成并交付成功后，可在“我的已购”中重新查看和复制兑换码。", "前往我的已购");
            }
            if ("SUBSCRIPTION".equals(type)) {
                return new DeliveryAccess("支付完成后订阅权益会自动激活，可在“我的已购”查看有效期和交付记录。", "查看权益记录");
            }
            return new DeliveryAccess("支付完成后，文件、链接或账号信息会进入“我的已购”，后续可随时重新打开。", "查看已购内容");
        }
        if ("OFFLINE_SERVICE".equals(mode)) {
            return new DeliveryAccess("支付完成后会生成服务核销凭证，可在“我的已购”中向商户出示或复制核销码。", "查看服务凭证");
        }
        return new DeliveryAccess("支付完成后，发货进度和物流信息会同步到订单详情和“我的已购”。", "查看订单履约");
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
