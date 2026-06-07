package com.payment.service;

import com.payment.entity.MemberLevel;
import com.payment.entity.MemberTag;

import java.math.BigDecimal;
import java.util.List;

/**
 * 会员运营服务，管理租户会员等级和标签。
 */
public interface MemberService {

    List<MemberLevel> listLevels(Long tenantId);

    MemberLevel createLevel(Long tenantId, Integer level, String name, BigDecimal thresholdAmount, BigDecimal discountRate);

    void updateMemberLevel(Long tenantId, Long memberId, Integer memberLevel);

    List<MemberTag> listTags(Long tenantId);

    MemberTag createTag(Long tenantId, String name);

    void assignTag(Long tenantId, Long memberId, Long tagId);

    void removeTag(Long tenantId, Long memberId, Long tagId);

    /**
     * 根据用户累计消费金额自动检查并升级会员等级。
     * 如果累计消费达到更高等级门槛，则自动升级。
     *
     * @param tenantId     租户ID
     * @param platformUserId 平台用户ID
     */
    void checkAndAutoUpgrade(Long tenantId, Long platformUserId);
}
