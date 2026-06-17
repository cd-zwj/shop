package com.payment.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.payment.dto.V1MerchantStoreUpsertDTO;
import com.payment.dto.V1MerchantStoreVO;

public interface V1MerchantStoreService {

    Page<V1MerchantStoreVO> listStores(Long tenantId, Long platformUserId, Integer current, Integer size,
                                       String keyword, Integer status);

    V1MerchantStoreVO getStore(Long tenantId, Long platformUserId, Long storeId);

    V1MerchantStoreVO createStore(Long tenantId, Long platformUserId, V1MerchantStoreUpsertDTO dto);

    V1MerchantStoreVO updateStore(Long tenantId, Long platformUserId, Long storeId, V1MerchantStoreUpsertDTO dto);

    V1MerchantStoreVO updateStoreStatus(Long tenantId, Long platformUserId, Long storeId, Integer status);

    void deleteStore(Long tenantId, Long platformUserId, Long storeId);
}
