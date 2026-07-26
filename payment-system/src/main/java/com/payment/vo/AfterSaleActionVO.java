package com.payment.vo;

import com.fasterxml.jackson.core.type.TypeReference;
import com.payment.entity.AfterSaleAction;
import com.payment.util.JsonUtils;
import lombok.Builder;
import lombok.Data;

import java.util.List;

/** 售后处理流水对外视图。 */
@Data
@Builder
public class AfterSaleActionVO {
    private String action;
    private String operatorRole;
    private String remark;
    private List<String> evidenceUrls;
    private String createTime;

    public static AfterSaleActionVO from(AfterSaleAction action) {
        return AfterSaleActionVO.builder()
                .action(action.getAction())
                .operatorRole(action.getOperatorRole())
                .remark(action.getRemark())
                .evidenceUrls(action.getEvidenceUrlsJson() == null || action.getEvidenceUrlsJson().isBlank()
                        ? List.of() : JsonUtils.fromJson(action.getEvidenceUrlsJson(), new TypeReference<List<String>>() { }))
                .createTime(VoConverterUtil.formatTime(action.getCreateTime()))
                .build();
    }
}
