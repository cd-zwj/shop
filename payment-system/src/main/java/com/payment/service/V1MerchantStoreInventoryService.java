package com.payment.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.payment.dto.V1MerchantStoreInventoryAdjustDTO;
import com.payment.dto.V1MerchantStoreInventoryLogVO;
import com.payment.dto.V1MerchantStoreInventoryVO;

/**
 * 商户端门店库存管理。
 */
public interface V1MerchantStoreInventoryService {

    Page<V1MerchantStoreInventoryVO> listStocks(Long tenantId, Long platformUserId, Integer current, Integer size,
                                                Long storeId, Long productId, Boolean lowStockOnly, Integer threshold);

    V1MerchantStoreInventoryVO adjustStock(Long tenantId, Long platformUserId,
                                            V1MerchantStoreInventoryAdjustDTO dto);

    Page<V1MerchantStoreInventoryLogVO> listChangeLogs(Long tenantId, Long platformUserId, Integer current,
                                                        Integer size, Long storeId, Long productId);
}
