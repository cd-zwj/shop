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
 * 提现服务接口
 */
public interface WithdrawalService {
    
    /**
     * 创建提现申请
     */
    Withdrawal createWithdrawal(Long tenantId, WithdrawalApplyDTO dto);
    
    /**
     * 查询提现申请列表
     */
    Page<Withdrawal> listWithdrawals(WithdrawalQueryDTO query);
    
    /**
     * 审核提现申请
     */
    void approveWithdrawal(Long approverId, WithdrawalApproveDTO dto);
    
    /**
     * 查询商家余额
     */
    MerchantBalance getMerchantBalance(Long tenantId);
    
    /**
     * 增加商家余额
     */
    void addMerchantBalance(Long tenantId, BigDecimal amount, String orderNo);
    
    /**
     * 扣减商家余额
     */
    void deductMerchantBalance(Long tenantId, BigDecimal amount);
    
    /**
     * 管理员查询提现申请列表（带商家名称）
     */
    Page<WithdrawalVO> listWithdrawalsForAdmin(Integer current, Integer size,
                                               String merchantName, Integer status, String startDate, String endDate);
    
    /**
     * 审核通过提现申请（简化版）
     */
    void approveWithdrawal(Long withdrawalId);
    
    /**
     * 拒绝提现申请
     */
    void rejectWithdrawal(Long withdrawalId, String reason);
}
