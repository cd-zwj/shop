package com.payment.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.payment.entity.MemberGrowthLog;
import com.payment.vo.MemberGrowthAccountVO;

/**
 * 会员成长值服务接口。
 *
 * <p>管理会员成长值的增加、扣减和等级自动升级。成长值是衡量用户活跃度和忠诚度的指标，
 * 通过消费、充值等行为累积，达到阈值后自动提升会员等级。
 * 成长值支持升级和降级两种方向的等级调整。</p>
 */
public interface MemberGrowthService {

    /**
     * 增加成长值并记录日志。
     *
     * @param platformUserId 平台用户ID
     * @param tenantId       租户ID
     * @param growthAmount   增加的成长值（正整数）
     * @param sourceType     业务类型：ORDER / RECHARGE / MANUAL
     * @param sourceBizNo    业务单号
     * @param description    备注说明
     */
    void addGrowth(Long platformUserId, Long tenantId, int growthAmount,
                   String sourceType, String sourceBizNo, String description);

    /**
     * 扣减成长值并记录日志。
     *
     * @param platformUserId 平台用户ID
     * @param tenantId       租户ID
     * @param growthAmount   扣减的成长值（正整数）
     * @param sourceType     业务类型
     * @param sourceBizNo    业务单号
     * @param description    备注说明
     */
    void deductGrowth(Long platformUserId, Long tenantId, int growthAmount,
                      String sourceType, String sourceBizNo, String description);

    /**
     * 查询当前成长值总额（从日志表聚合计算）。
     *
     * @param platformUserId 平台用户ID
     * @param tenantId       租户ID
     * @return 成长值总额
     */
    int getTotalGrowth(Long platformUserId, Long tenantId);

    /**
     * 查询成长值概览（总额 + 当前等级 + 下一级阈值）。
     *
     * @param platformUserId 平台用户ID
     * @param tenantId       租户ID
     * @return 成长值概览视图对象
     */
    MemberGrowthAccountVO getGrowthAccount(Long platformUserId, Long tenantId);

    /**
     * 分页查询成长值变更日志。
     *
     * @param platformUserId 平台用户ID
     * @param tenantId       租户ID
     * @param pageNum        页码
     * @param pageSize       每页条数
     * @return 成长值日志分页数据
     */
    Page<MemberGrowthLog> listGrowthLogs(Long platformUserId, Long tenantId,
                                         Integer pageNum, Integer pageSize);

    /**
     * 检查是否满足升级条件，满足则自动升级会员等级（仅升级）。
     *
     * @param platformUserId 平台用户ID
     * @param tenantId       租户ID
     * @return 调整后的等级ID，null 表示未达到升级条件
     */
    Long checkAndUpgradeLevel(Long platformUserId, Long tenantId);

    /**
     * 按当前成长值重新计算目标等级，支持升级和降级。
     *
     * <p>当成长值因扣减导致低于当前等级门槛时，会触发降级处理。</p>
     *
     * @param platformUserId 平台用户ID
     * @param tenantId       租户ID
     * @return 调整后的等级ID，null 表示等级未变化
     */
    Long checkAndAdjustLevel(Long platformUserId, Long tenantId);
}
