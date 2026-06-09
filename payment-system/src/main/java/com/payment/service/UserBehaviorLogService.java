package com.payment.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.payment.entity.UserBehaviorLog;

/**
 * 用户行为日志服务接口。
 */
public interface UserBehaviorLogService {

    /**
     * 记录用户行为（异步安全，调用方应 try-catch 包裹）。
     *
     * @param platformUserId 平台用户ID
     * @param tenantId       租户ID
     * @param behaviorType   行为类型：VIEW/CLICK/SEARCH/ADD_CART/PURCHASE/SHARE/FAVORITE
     * @param targetType     目标类型：PRODUCT/TENANT/COUPON/ACTIVITY（可为null）
     * @param targetId       目标ID（可为null）
     * @param detail         附加详情 JSON（可为null）
     */
    void recordBehavior(Long platformUserId,
                        Long tenantId,
                        String behaviorType,
                        String targetType,
                        Long targetId,
                        String detail);

    /**
     * 按用户分页查询行为日志。
     */
    Page<UserBehaviorLog> listByUser(Long platformUserId,
                                     Long tenantId,
                                     String behaviorType,
                                     int page,
                                     int size);

    /**
     * 按目标分页查询行为日志。
     */
    Page<UserBehaviorLog> listByTarget(String targetType,
                                       Long targetId,
                                       int page,
                                       int size);
}
