package com.payment.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.payment.common.BusinessException;
import com.payment.dto.WithdrawalApplyDTO;
import com.payment.dto.WithdrawalApproveDTO;
import com.payment.dto.WithdrawalQueryDTO;
import com.payment.entity.MerchantBalance;
import com.payment.entity.Withdrawal;
import com.payment.mapper.MerchantBalanceMapper;
import com.payment.mapper.WithdrawalMapper;
import com.payment.service.WithdrawalService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 提现服务实现类
 */
@Slf4j
@Service
public class WithdrawalServiceImpl implements WithdrawalService {
    
    @Autowired
    private WithdrawalMapper withdrawalMapper;
    
    @Autowired
    private MerchantBalanceMapper merchantBalanceMapper;
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Withdrawal createWithdrawal(Long tenantId, WithdrawalApplyDTO dto) {
        if (tenantId == null) {
            throw new BusinessException("租户信息不存在");
        }
        
        // 查询商家余额
        MerchantBalance balance = getMerchantBalance(tenantId);
        if (balance == null || balance.getBalance().compareTo(dto.getAmount()) < 0) {
            throw new BusinessException("可提现余额不足");
        }
        
        // 创建提现申请
        Withdrawal withdrawal = new Withdrawal();
        BeanUtils.copyProperties(dto, withdrawal);
        withdrawal.setTenantId(tenantId);
        withdrawal.setStatus(0); // 待审核
        withdrawal.setDeleted(0);
        withdrawal.setApplyTime(LocalDateTime.now());
        withdrawal.setCreateTime(LocalDateTime.now());
        withdrawalMapper.insert(withdrawal);
        
        log.info("创建提现申请，tenantId={}, amount={}, withdrawalId={}", 
                tenantId, dto.getAmount(), withdrawal.getId());
        
        return withdrawal;
    }
    
    @Override
    public Page<Withdrawal> listWithdrawals(WithdrawalQueryDTO query) {
        Page<Withdrawal> page = new Page<>(query.getPageNum(), query.getPageSize());
        
        LambdaQueryWrapper<Withdrawal> wrapper = new LambdaQueryWrapper<Withdrawal>()
                .eq(query.getTenantId() != null, Withdrawal::getTenantId, query.getTenantId())
                .eq(query.getStatus() != null, Withdrawal::getStatus, query.getStatus())
                .eq(Withdrawal::getDeleted, 0)
                .orderByDesc(Withdrawal::getApplyTime);
        
        return withdrawalMapper.selectPage(page, wrapper);
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void approveWithdrawal(Long approverId, WithdrawalApproveDTO dto) {
        if (approverId == null) {
            throw new BusinessException("审核人信息不存在");
        }
        
        // 查询提现申请
        Withdrawal withdrawal = withdrawalMapper.selectOne(
                new LambdaQueryWrapper<Withdrawal>()
                        .eq(Withdrawal::getId, dto.getWithdrawalId())
                        .eq(Withdrawal::getDeleted, 0)
        );
        
        if (withdrawal == null) {
            throw new BusinessException("提现申请不存在");
        }
        
        if (withdrawal.getStatus() != 0) {
            throw new BusinessException("提现申请已审核");
        }
        
        if (dto.getApproved()) {
            // 审核通过
            // 查询商家余额
            MerchantBalance balance = getMerchantBalance(withdrawal.getTenantId());
            if (balance == null || balance.getBalance().compareTo(withdrawal.getAmount()) < 0) {
                throw new BusinessException("商家余额不足，无法通过提现申请");
            }
            
            // 扣减商家余额
            balance.setBalance(balance.getBalance().subtract(withdrawal.getAmount()));
            balance.setTotalWithdrawal(balance.getTotalWithdrawal().add(withdrawal.getAmount()));
            balance.setUpdateTime(LocalDateTime.now());
            merchantBalanceMapper.updateById(balance);
            
            // 更新提现状态
            withdrawal.setStatus(1); // 已通过
            log.info("提现审核通过，withdrawalId={}, tenantId={}, amount={}", 
                    dto.getWithdrawalId(), withdrawal.getTenantId(), withdrawal.getAmount());
        } else {
            // 审核拒绝
            if (dto.getRejectReason() == null || dto.getRejectReason().trim().isEmpty()) {
                throw new BusinessException("拒绝原因不能为空");
            }
            withdrawal.setStatus(2); // 已拒绝
            withdrawal.setRejectReason(dto.getRejectReason());
            log.info("提现审核拒绝，withdrawalId={}, tenantId={}, reason={}", 
                    dto.getWithdrawalId(), withdrawal.getTenantId(), dto.getRejectReason());
        }
        
        withdrawal.setApproverId(approverId);
        withdrawal.setApproveTime(LocalDateTime.now());
        withdrawalMapper.updateById(withdrawal);
    }
    
    @Override
    public MerchantBalance getMerchantBalance(Long tenantId) {
        return merchantBalanceMapper.selectOne(
                new LambdaQueryWrapper<MerchantBalance>()
                        .eq(MerchantBalance::getTenantId, tenantId)
                        .eq(MerchantBalance::getDeleted, 0)
        );
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void addMerchantBalance(Long tenantId, BigDecimal amount, String orderNo) {
        if (tenantId == null) {
            throw new BusinessException("租户信息不存在");
        }
        
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            log.warn("增加商家余额金额无效，tenantId={}, amount={}", tenantId, amount);
            return;
        }
        
        // 查询或创建商家余额记录
        MerchantBalance balance = getMerchantBalance(tenantId);
        
        if (balance == null) {
            // 创建商家余额记录
            balance = new MerchantBalance();
            balance.setTenantId(tenantId);
            balance.setBalance(amount);
            balance.setFrozenBalance(BigDecimal.ZERO);
            balance.setTotalIncome(amount);
            balance.setTotalWithdrawal(BigDecimal.ZERO);
            balance.setDeleted(0);
            balance.setCreateTime(LocalDateTime.now());
            balance.setUpdateTime(LocalDateTime.now());
            merchantBalanceMapper.insert(balance);
        } else {
            // 更新商家余额
            balance.setBalance(balance.getBalance().add(amount));
            balance.setTotalIncome(balance.getTotalIncome().add(amount));
            balance.setUpdateTime(LocalDateTime.now());
            merchantBalanceMapper.updateById(balance);
        }
        
        log.info("增加商家余额成功，tenantId={}, amount={}, balance={}, orderNo={}", 
                tenantId, amount, balance.getBalance(), orderNo);
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deductMerchantBalance(Long tenantId, BigDecimal amount) {
        if (tenantId == null) {
            throw new BusinessException("租户信息不存在");
        }
        
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException("扣减金额必须大于0");
        }
        
        // 查询商家余额
        MerchantBalance balance = getMerchantBalance(tenantId);
        if (balance == null || balance.getBalance().compareTo(amount) < 0) {
            throw new BusinessException("商家余额不足");
        }
        
        // 扣减余额
        balance.setBalance(balance.getBalance().subtract(amount));
        balance.setUpdateTime(LocalDateTime.now());
        merchantBalanceMapper.updateById(balance);
        
        log.info("扣减商家余额成功，tenantId={}, amount={}, balance={}", 
                tenantId, amount, balance.getBalance());
    }
    
    @Override
    public Page<com.payment.dto.WithdrawalVO> listWithdrawalsForAdmin(Integer current, Integer size, 
            String merchantName, Integer status, String startDate, String endDate) {
        Page<Withdrawal> page = new Page<>(current, size);
        
        LambdaQueryWrapper<Withdrawal> wrapper = new LambdaQueryWrapper<Withdrawal>()
                .eq(status != null, Withdrawal::getStatus, status)
                .ge(startDate != null, Withdrawal::getApplyTime, startDate)
                .le(endDate != null, Withdrawal::getApplyTime, endDate)
                .eq(Withdrawal::getDeleted, 0)
                .orderByDesc(Withdrawal::getApplyTime);
        
        Page<Withdrawal> withdrawalPage = withdrawalMapper.selectPage(page, wrapper);
        
        // 转换为VO并关联商家名称
        Page<com.payment.dto.WithdrawalVO> voPage = new Page<>(withdrawalPage.getCurrent(), 
                withdrawalPage.getSize(), withdrawalPage.getTotal());
        
        java.util.List<com.payment.dto.WithdrawalVO> voList = withdrawalPage.getRecords().stream()
                .map(withdrawal -> {
                    com.payment.dto.WithdrawalVO vo = new com.payment.dto.WithdrawalVO();
                    org.springframework.beans.BeanUtils.copyProperties(withdrawal, vo);
                    
                    // 查询商家名称
                    com.payment.entity.Tenant tenant = getTenantById(withdrawal.getTenantId());
                    if (tenant != null) {
                        vo.setMerchantName(tenant.getName());
                    }
                    
                    // 查询审核人名称（如果有）
                    if (withdrawal.getApproverId() != null) {
                        com.payment.entity.User approver = getUserById(withdrawal.getApproverId());
                        if (approver != null) {
                            vo.setApproverName(approver.getUsername());
                        }
                    }
                    
                    return vo;
                })
                .filter(vo -> merchantName == null || merchantName.isEmpty() 
                        || (vo.getMerchantName() != null && vo.getMerchantName().contains(merchantName)))
                .collect(java.util.stream.Collectors.toList());
        
        voPage.setRecords(voList);
        
        return voPage;
    }
    
    /**
     * 根据ID查询租户
     */
    private com.payment.entity.Tenant getTenantById(Long tenantId) {
        try {
            com.payment.mapper.TenantMapper tenantMapper = 
                    com.payment.util.SpringContextUtil.getBean(com.payment.mapper.TenantMapper.class);
            return tenantMapper.selectById(tenantId);
        } catch (Exception e) {
            log.error("查询租户失败，tenantId={}", tenantId, e);
            return null;
        }
    }
    
    /**
     * 根据ID查询用户
     */
    private com.payment.entity.User getUserById(Long userId) {
        try {
            com.payment.mapper.UserMapper userMapper = 
                    com.payment.util.SpringContextUtil.getBean(com.payment.mapper.UserMapper.class);
            return userMapper.selectById(userId);
        } catch (Exception e) {
            log.error("查询用户失败，userId={}", userId, e);
            return null;
        }
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void approveWithdrawal(Long withdrawalId) {
        // 查询提现申请
        Withdrawal withdrawal = withdrawalMapper.selectOne(
                new LambdaQueryWrapper<Withdrawal>()
                        .eq(Withdrawal::getId, withdrawalId)
                        .eq(Withdrawal::getDeleted, 0)
        );
        
        if (withdrawal == null) {
            throw new BusinessException("提现申请不存在");
        }
        
        if (withdrawal.getStatus() != 0) {
            throw new BusinessException("提现申请已审核");
        }
        
        // 查询商家余额
        MerchantBalance balance = getMerchantBalance(withdrawal.getTenantId());
        if (balance == null || balance.getBalance().compareTo(withdrawal.getAmount()) < 0) {
            throw new BusinessException("商家余额不足，无法通过提现申请");
        }
        
        // 扣减商家余额
        balance.setBalance(balance.getBalance().subtract(withdrawal.getAmount()));
        balance.setTotalWithdrawal(balance.getTotalWithdrawal().add(withdrawal.getAmount()));
        balance.setUpdateTime(LocalDateTime.now());
        merchantBalanceMapper.updateById(balance);
        
        // 更新提现状态
        withdrawal.setStatus(1); // 已通过
        withdrawal.setApproveTime(LocalDateTime.now());
        withdrawalMapper.updateById(withdrawal);
        
        log.info("提现审核通过，withdrawalId={}, tenantId={}, amount={}", 
                withdrawalId, withdrawal.getTenantId(), withdrawal.getAmount());
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void rejectWithdrawal(Long withdrawalId, String reason) {
        if (reason == null || reason.trim().isEmpty()) {
            throw new BusinessException("拒绝原因不能为空");
        }
        
        // 查询提现申请
        Withdrawal withdrawal = withdrawalMapper.selectOne(
                new LambdaQueryWrapper<Withdrawal>()
                        .eq(Withdrawal::getId, withdrawalId)
                        .eq(Withdrawal::getDeleted, 0)
        );
        
        if (withdrawal == null) {
            throw new BusinessException("提现申请不存在");
        }
        
        if (withdrawal.getStatus() != 0) {
            throw new BusinessException("提现申请已审核");
        }
        
        // 更新提现状态
        withdrawal.setStatus(2); // 已拒绝
        withdrawal.setRejectReason(reason);
        withdrawal.setApproveTime(LocalDateTime.now());
        withdrawalMapper.updateById(withdrawal);
        
        log.info("提现审核拒绝，withdrawalId={}, tenantId={}, reason={}", 
                withdrawalId, withdrawal.getTenantId(), reason);
    }
}
