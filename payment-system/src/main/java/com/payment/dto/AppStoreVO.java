package com.payment.dto;

import com.payment.entity.Store;
import lombok.Data;

/** 用户端可选自提门店。 */
@Data
public class AppStoreVO {
    private Long id;
    private Long tenantId;
    private String storeName;
    private String contactPhone;
    private String address;
    private String businessHours;

    public static AppStoreVO from(Store store) {
        if (store == null) {
            return null;
        }
        AppStoreVO vo = new AppStoreVO();
        vo.setId(store.getId());
        vo.setTenantId(store.getTenantId());
        vo.setStoreName(store.getStoreName());
        vo.setContactPhone(store.getContactPhone());
        vo.setAddress(store.getAddress());
        vo.setBusinessHours(store.getBusinessHours());
        return vo;
    }
}
