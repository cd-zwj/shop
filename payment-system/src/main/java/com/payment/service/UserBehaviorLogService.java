package com.payment.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.payment.entity.UserBehaviorLog;

/**
 * 用户行为日志服务接口。
 *
 * <p>负责记录和查询用户在平台上的各类行为（浏览、点击、搜索、加购、购买等），
 * 用于用户画像分析、推荐系统和营销策略的数据支撑。
 * 行为记录采用异步方式，不阻塞主业务流程。</p>
 */
public interface UserBehaviorLogService {

    /**
     * 记录用户行为（异步安全，调用方应 try-catch 包裹以避免埋点失败影响主流程）。
     *
     * @param platformUserId 平台用户ID
     * @param tenantId       租户ID
     * @param behaviorType   行为类型：VIEW / CLICK / SEARCH / ADD_CART / PURCHASE / SHARE / FAVORITE
     * @param targetType     目标类型：PRODUCT / TENANT / COUPON / ACTIVITY（可为null）
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
     *
     * @param platformUserId 平台用户ID
     * @param tenantId       租户ID（可为null）
     * @param behaviorType   行为类型筛选（可为null）
     * @param page           当前页码
     * @param size           每页条数
     * @return 行为日志分页结果
     */
    Page<UserBehaviorLog> listByUser(Long platformUserId,
                                     Long tenantId,
                                     String behaviorType,
                                     int page,
                                     int size);

    /**
     * 按目标对象分页查询行为日志（用于查看某个商品/活动的行为数据）。
     *
     * @param targetType 目标类型：PRODUCT / TENANT / COUPON / ACTIVITY
     * @param targetId   目标ID
     * @param page       当前页码
     * @param size       每页条数
     * @return 行为日志分页结果
     */
    Page<UserBehaviorLog> listByTarget(String targetType,
                                       Long targetId,
                                       int page,
                                       int size);
}
