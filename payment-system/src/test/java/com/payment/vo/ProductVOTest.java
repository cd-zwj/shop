package com.payment.vo;

import com.payment.entity.Product;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class ProductVOTest {

    @Test
    void cardKeyProductShouldExposeDeliveryAndAfterSalesGuidance() {
        Product product = product("CARD_KEY", "ONLINE_VIRTUAL", 8, 1);

        ProductVO vo = ProductVO.from(product);

        assertThat(vo.getInventoryLabel()).isEqualTo("库存充足");
        assertThat(vo.getInventoryDescription()).isEqualTo("当前可售 8 件");
        assertThat(vo.getFulfillmentLabel()).isEqualTo("卡密自动交付");
        assertThat(vo.getFulfillmentDescription()).contains("系统会自动发放可用卡密");
        assertThat(vo.getAfterSalesNote()).contains("卡密未使用前");
        assertThat(vo.getPurchaseLimitNote()).contains("当前库存 8 件");
        assertThat(vo.getDeliveryAccessDescription()).contains("我的已购").contains("兑换码");
        assertThat(vo.getDeliveryAccessActionLabel()).isEqualTo("前往我的已购");
        assertThat(vo.getPurchasable()).isTrue();
    }

    @Test
    void outOfStockPhysicalProductShouldBlockPurchase() {
        Product product = product("PHYSICAL", "EXPRESS_DELIVERY", 0, 1);

        ProductVO vo = ProductVO.from(product);

        assertThat(vo.getInventoryLabel()).isEqualTo("暂时缺货");
        assertThat(vo.getPurchaseLimitNote()).contains("当前不可购买");
        assertThat(vo.getFulfillmentLabel()).isEqualTo("快递发货");
        assertThat(vo.getDeliveryAccessDescription()).contains("订单详情");
        assertThat(vo.getPurchasable()).isFalse();
    }

    @Test
    void offlineServiceShouldExposeVoucherAccess() {
        Product product = product("SERVICE", "OFFLINE_SERVICE", 3, 1);
        product.setUnit("次");

        ProductVO vo = ProductVO.from(product);

        assertThat(vo.getInventoryDescription()).isEqualTo("当前可售 3 次");
        assertThat(vo.getFulfillmentLabel()).isEqualTo("线下服务");
        assertThat(vo.getAfterSalesNote()).contains("服务未核销前");
        assertThat(vo.getDeliveryAccessDescription()).contains("核销码");
        assertThat(vo.getDeliveryAccessActionLabel()).isEqualTo("查看服务凭证");
    }

    private Product product(String productType, String fulfillmentMode, Integer stock, Integer status) {
        Product product = new Product();
        product.setId(1L);
        product.setTenantId(9L);
        product.setName("测试商品");
        product.setPrice(new BigDecimal("10.00"));
        product.setUnit("件");
        product.setProductType(productType);
        product.setFulfillmentMode(fulfillmentMode);
        product.setStock(stock);
        product.setStatus(status);
        product.setDeleted(0);
        return product;
    }
}
