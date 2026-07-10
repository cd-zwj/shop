package com.payment.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.payment.dto.WithdrawalApplyDTO;
import com.payment.dto.WithdrawalApproveDTO;
import com.payment.dto.WithdrawalQueryDTO;
import com.payment.dto.WithdrawalVO;
import com.payment.entity.MerchantBalance;
import com.payment.entity.Withdrawal;

import java.math.BigDecimal;

/**
 * 提现服务接口。
 * <p>
 * 管理商户提现的完整流程，包括提现申请创建、审核、商家余额管理，
 * 以及管理员端的提现列表查询和审批操作。
 */
public interface WithdrawalService {

    /**
     * 创建提现申请。
     *
     * @param tenantId 租户 ID
     * @param dto      提现申请 DTO，包含提现金额、收款账户等信息
     * @return 创建的提现申请实体
     */
    Withdrawal createWithdrawal(Long tenantId, WithdrawalApplyDTO dto);

    /**
     * 查询提现申请列表。
     *
     * @param query 查询条件 DTO（包含状态、时间范围等筛选条件）
     * @return 提现申请分页结果
     */
    Page<Withdrawal> listWithdrawals(WithdrawalQueryDTO query);

    /**
     * 审核提现申请（通用版，带审批意见）。
     *
     * @param approverId 审批人 ID
     * @param dto        审批 DTO，包含提现申请 ID、审批结果和意见
     */
    void approveWithdrawal(Long approverId, WithdrawalApproveDTO dto);

    /**
     * 查询商家余额信息。
     *
     * @param tenantId 租户 ID
     * @return 商家余额实体
     */
    MerchantBalance getMerchantBalance(Long tenantId);

    /**
     * 增加商家余额。
     * <p>
     * 用于订单结算入账等场景。
     *
     * @param tenantId 租户 ID
     * @param amount   增加金额（单位：元）
     * @param orderNo  关联订单号
     */
    void addMerchantBalance(Long tenantId, BigDecimal amount, String orderNo);

    /**
     * 增加商家余额并累计平台服务费。
     * <p>
     * 用于订单结算入账。amount 为扣除平台服务费后的净入账金额，
     * platformFee 为本次订单结算产生的平台抽成金额。
     *
     * @param tenantId     租户 ID
     * @param amount       净入账金额（单位：元）
     * @param orderNo      关联订单号
     * @param platformFee  平台服务费（单位：元）
     */
    void addMerchantBalance(Long tenantId, BigDecimal amount, String orderNo, BigDecimal platformFee);

    /**
     * 扣减商家余额。
     * <p>
     * 用于提现等场景，需确保余额充足。
     *
     * @param tenantId 租户 ID
     * @param amount   扣减金额（单位：元）
     */
    void deductMerchantBalance(Long tenantId, BigDecimal amount);

    /**
     * 管理员查询提现申请列表（带商家名称信息）。
     *
     * @param current      当前页码
     * @param size         每页数量
     * @param merchantName 商家名称筛选（可选，模糊匹配）
     * @param status       提现状态筛选（可选）
     * @param startDate    开始日期筛选（可选，格式：yyyy-MM-dd）
     * @param endDate      结束日期筛选（可选，格式：yyyy-MM-dd）
     * @return 提现申请视图分页结果
     */
    Page<WithdrawalVO> listWithdrawalsForAdmin(Integer current, Integer size,
                                               String merchantName, Integer status, String startDate, String endDate);

    /**
     * 审核通过提现申请（简化版）。
     * <p>
     * 直接通过指定提现申请，无需额外审批意见。
     *
     * @param withdrawalId 提现申请 ID
     */
    void approveWithdrawal(Long withdrawalId);

    /**
     * 拒绝提现申请。
     *
     * @param withdrawalId 提现申请 ID
     * @param reason       拒绝原因
     */
    void rejectWithdrawal(Long withdrawalId, String reason);
}
