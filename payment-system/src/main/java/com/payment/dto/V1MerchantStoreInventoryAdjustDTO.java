package com.payment.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 商户手工调整门店库存。
 */
@Data
public class V1MerchantStoreInventoryAdjustDTO {

    @NotNull(message = "门店不能为空")
    private Long storeId;

    @NotNull(message = "商品不能为空")
    private Long productId;

    /** 正数为入库，负数为盘亏/出库。 */
    @NotNull(message = "调整数量不能为空")
    private Integer delta;

    private String remark;
}
