package com.payment.service;

import com.payment.entity.MemberLevel;
import com.payment.entity.MemberTag;

import java.math.BigDecimal;
import java.util.List;

/**
 * 会员运营服务接口。
 *
 * <p>管理租户的会员等级体系和会员标签系统。会员等级与消费门槛挂钩，
 * 支持自动升级；标签用于对会员进行分组标记，便于精细化运营。</p>
 */
public interface MemberService {

    /**
     * 查询租户下的会员等级列表。
     *
     * @param tenantId 租户ID
     * @return 会员等级列表，按等级值升序排列
     */
    List<MemberLevel> listLevels(Long tenantId);

    /**
     * 创建会员等级。
     *
     * @param tenantId        租户ID
     * @param level           等级数值（越大等级越高）
     * @param name            等级名称（如：普通会员、银卡会员、金卡会员）
     * @param thresholdAmount 消费门槛金额（累计消费达到此金额可升级）
     * @param discountRate    会员折扣率（如 0.95 表示九五折）
     * @return 创建成功的会员等级实体
     * @throws com.payment.common.exception.BusinessException 当等级数值重复时抛出
     */
    MemberLevel createLevel(Long tenantId, Integer level, String name, BigDecimal thresholdAmount, BigDecimal discountRate);

    /**
     * 手动调整用户会员等级。
     *
     * @param tenantId    租户ID
     * @param memberId    会员记录ID
     * @param memberLevel 目标等级值
     * @throws com.payment.common.exception.BusinessException 当目标等级不存在时抛出
     */
    void updateMemberLevel(Long tenantId, Long memberId, Integer memberLevel);

    /**
     * 查询租户下的会员标签列表。
     *
     * @param tenantId 租户ID
     * @return 标签列表
     */
    List<MemberTag> listTags(Long tenantId);

    /**
     * 创建会员标签。
     *
     * @param tenantId 租户ID
     * @param name     标签名称
     * @return 创建成功的标签实体
     * @throws com.payment.common.exception.BusinessException 当标签名称重复时抛出
     */
    MemberTag createTag(Long tenantId, String name);

    /**
     * 为会员分配标签。
     *
     * @param tenantId 租户ID
     * @param memberId 会员记录ID
     * @param tagId    标签ID
     */
    void assignTag(Long tenantId, Long memberId, Long tagId);

    /**
     * 移除会员标签。
     *
     * @param tenantId 租户ID
     * @param memberId 会员记录ID
     * @param tagId    标签ID
     */
    void removeTag(Long tenantId, Long memberId, Long tagId);

    /**
     * 根据用户累计消费金额自动检查并升级会员等级。
     *
     * <p>遍历租户配置的会员等级列表，当累计消费金额达到更高等级门槛时自动升级。
     * 通常在订单完成后由消息队列异步触发。</p>
     *
     * @param tenantId       租户ID
     * @param platformUserId 平台用户ID
     */
    void checkAndAutoUpgrade(Long tenantId, Long platformUserId);
}
