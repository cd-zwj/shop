package com.payment.dto;

import com.payment.entity.ProductChangeLog;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 商户端商品变更流水视图。
 */
@Data
public class V1MerchantProductChangeLogVO {

    private Long id;
    private Long tenantId;
    private Long productId;
    private String changeType;
    private String fieldName;
    private String oldValue;
    private String newValue;
    private Long operatorId;
    private String remark;
    private LocalDateTime createTime;

    public static V1MerchantProductChangeLogVO from(ProductChangeLog log) {
        V1MerchantProductChangeLogVO vo = new V1MerchantProductChangeLogVO();
        vo.setId(log.getId());
        vo.setTenantId(log.getTenantId());
        vo.setProductId(log.getProductId());
        vo.setChangeType(log.getChangeType());
        vo.setFieldName(log.getFieldName());
        vo.setOldValue(log.getOldValue());
        vo.setNewValue(log.getNewValue());
        vo.setOperatorId(log.getOperatorId());
        vo.setRemark(log.getRemark());
        vo.setCreateTime(log.getCreateTime());
        return vo;
    }
}
