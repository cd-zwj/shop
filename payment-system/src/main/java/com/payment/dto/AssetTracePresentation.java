package com.payment.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 用户资产明细的展示契约。
 *
 * <p>用于钱包、积分、成长值、优惠券等资产流水，给前端提供稳定的来源、影响、状态和跳转入口。</p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AssetTracePresentation {

    private String title;
    private String source;
    private String effect;
    private String balance;
    private String status;
    private String hint;
    private String actionLabel;
    private String actionPath;
    private String inactiveActionLabel;
    private String tone;
}
