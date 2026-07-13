package com.payment.service;

import com.payment.dto.AppTenantAssetSummaryVO;
import com.payment.dto.AppAssetActivityVO;
import com.payment.dto.AssetActivityPageVO;
import com.payment.dto.AssetActivityQueryDTO;
import com.payment.dto.AssetHoldVO;

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

    /**
     * 查询当前用户的统一资产动态流（带筛选、游标分页和稳定排序）。
     *
     * @param platformUserId 平台用户ID
     * @param query          查询参数（类型、租户、时间范围、游标、条数）
     * @return 资产动态分页结果
     */
    AssetActivityPageVO listAssetActivities(Long platformUserId, AssetActivityQueryDTO query);

    /**
     * 查询当前用户的受限资产明细。
     *
     * @param platformUserId 平台用户ID
     * @param tenantId       可选商户ID，为空时查询全部商户
     * @return 锁定、预占或冻结资产
     */
    List<AssetHoldVO> listAssetHolds(Long platformUserId, Long tenantId);
}
