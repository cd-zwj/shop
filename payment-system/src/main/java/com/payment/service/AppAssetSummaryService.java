package com.payment.service;

import com.payment.dto.AppTenantAssetSummaryVO;
import com.payment.dto.AppAssetActivityVO;

import java.util.List;

/**
 * 用户端资产汇总服务。
 */
public interface AppAssetSummaryService {

    /**
     * 查询当前用户已有商户会员、商户钱包或积分账户的资产概览。
     *
     * @param platformUserId 平台用户ID
     * @return 商户资产概览列表
     */
    List<AppTenantAssetSummaryVO> listTenantAssetSummaries(Long platformUserId);

    /**
     * 查询当前用户的统一资产动态流。
     *
     * @param platformUserId 平台用户ID
     * @param size           返回条数
     * @return 资产动态列表
     */
    List<AppAssetActivityVO> listAssetActivities(Long platformUserId, Integer size);
}
