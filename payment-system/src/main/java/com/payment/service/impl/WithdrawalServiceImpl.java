package com.payment.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.payment.common.BusinessException;
import com.payment.dto.WithdrawalApplyDTO;
import com.payment.dto.WithdrawalApproveDTO;
import com.payment.dto.WithdrawalQueryDTO;
import com.payment.dto.WithdrawalVO;
import com.payment.entity.MerchantBalance;
import com.payment.entity.Tenant;
import com.payment.entity.User;
import com.payment.entity.Withdrawal;
import com.payment.mapper.MerchantBalanceMapper;
import com.payment.mapper.TenantEmployeeMapper;
import com.payment.mapper.TenantMapper;
import com.payment.mapper.UserMapper;
import com.payment.mapper.WithdrawalMapper;
import com.payment.service.UserNotificationService;
import com.payment.service.WithdrawalService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
public class WithdrawalServiceImpl implements WithdrawalService {

    @Autowired
    private WithdrawalMapper withdrawalMapper;

    @Autowired
    private MerchantBalanceMapper merchantBalanceMapper;

    @Autowired
    private TenantMapper tenantMapper;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private TenantEmployeeMapper tenantEmployeeMapper;

    @Autowired
    private UserNotificationService notificationService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Withdrawal createWithdrawal(Long tenantId, WithdrawalApplyDTO dto) {
        if (tenantId == null) {
            throw new BusinessException("租户信息不存在");
        }

        MerchantBalance balance = getMerchantBalance(tenantId);
        if (balance == null || balance.getBalance().compareTo(dto.getAmount()) < 0) {
            throw new BusinessException("可提现余额不足");
        }

        // 冻结余额（乐观锁重试）：balance → frozenBalance
        for (int attempt = 0; attempt < 3; attempt++) {
            balance.setBalance(balance.getBalance().subtract(dto.getAmount()));
            balance.setFrozenBalance(balance.getFrozenBalance().add(dto.getAmount()));
            balance.setUpdateTime(LocalDateTime.now());
            if (merchantBalanceMapper.updateById(balance) > 0) break;
            balance = getMerchantBalance(tenantId);
            if (balance == null || balance.getBalance().compareTo(dto.getAmount()) < 0) {
                throw new BusinessException("可提现余额不足");
            }
            if (attempt == 2) throw new BusinessException("操作冲突，请重试");
        }

        Withdrawal withdrawal = new Withdrawal();
        BeanUtils.copyProperties(dto, withdrawal);
        withdrawal.setTenantId(tenantId);
        withdrawal.setStatus(0);
        withdrawal.setDeleted(0);
        withdrawal.setApplyTime(LocalDateTime.now());
        withdrawal.setCreateTime(LocalDateTime.now());
        withdrawalMapper.insert(withdrawal);

        log.info("创建提现申请 tenantId={}, amount={}, withdrawalId={}",
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

        Withdrawal withdrawal = getPendingWithdrawal(dto.getWithdrawalId());
        if (Boolean.TRUE.equals(dto.getApproved())) {
            requireFrozenBalance(withdrawal, "冻结余额不足，无法通过提现申请");
            claimWithdrawalStatus(withdrawal, 1, approverId, null);
            moveFrozenBalanceToWithdrawal(withdrawal);
            log.info("提现审核通过 withdrawalId={}, tenantId={}, amount={}",
                    dto.getWithdrawalId(), withdrawal.getTenantId(), withdrawal.getAmount());
            return;
        }

        if (dto.getRejectReason() == null || dto.getRejectReason().trim().isEmpty()) {
            throw new BusinessException("拒绝原因不能为空");
        }
        requireFrozenBalance(withdrawal, "冻结余额不足，无法拒绝提现申请");
        claimWithdrawalStatus(withdrawal, 2, approverId, dto.getRejectReason());
        unfreezeWithdrawalBalance(withdrawal);
        log.info("提现审核拒绝 withdrawalId={}, tenantId={}, reason={}",
                dto.getWithdrawalId(), withdrawal.getTenantId(), dto.getRejectReason());
    }

    @Override
    public MerchantBalance getMerchantBalance(Long tenantId) {
        return merchantBalanceMapper.selectOne(new LambdaQueryWrapper<MerchantBalance>()
                .eq(MerchantBalance::getTenantId, tenantId)
                .eq(MerchantBalance::getDeleted, 0));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void addMerchantBalance(Long tenantId, BigDecimal amount, String orderNo) {
        if (tenantId == null) {
            throw new BusinessException("租户信息不存在");
        }
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            log.warn("增加商家余额金额无效 tenantId={}, amount={}", tenantId, amount);
            return;
        }

        MerchantBalance balance = getMerchantBalance(tenantId);
        if (balance == null) {
            balance = new MerchantBalance();
            balance.setTenantId(tenantId);
            balance.setBalance(amount);
            balance.setFrozenBalance(BigDecimal.ZERO);
            balance.setTotalIncome(amount);
            balance.setTotalWithdrawal(BigDecimal.ZERO);
            balance.setDeleted(0);
            balance.setCreateTime(LocalDateTime.now());
            balance.setUpdateTime(LocalDateTime.now());
            try {
                merchantBalanceMapper.insert(balance);
            } catch (Exception e) {
                // 并发创建 → DuplicateKeyException → 回退到重试更新
                log.warn("商家余额并发创建冲突，转为更新，tenantId={}", tenantId);
                balance = getMerchantBalance(tenantId);
                if (balance == null) throw new BusinessException("创建商家余额失败");
                for (int attempt = 0; attempt < 3; attempt++) {
                    balance.setBalance(balance.getBalance().add(amount));
                    balance.setTotalIncome(balance.getTotalIncome().add(amount));
                    balance.setUpdateTime(LocalDateTime.now());
                    if (merchantBalanceMapper.updateById(balance) > 0) break;
                    balance = merchantBalanceMapper.selectById(balance.getId());
                    if (attempt == 2) throw new BusinessException("操作冲突，请重试");
                }
            }
        } else {
            // 更新余额（乐观锁重试）
            for (int attempt = 0; attempt < 3; attempt++) {
                balance.setBalance(balance.getBalance().add(amount));
                balance.setTotalIncome(balance.getTotalIncome().add(amount));
                balance.setUpdateTime(LocalDateTime.now());
                if (merchantBalanceMapper.updateById(balance) > 0) break;
                balance = merchantBalanceMapper.selectById(balance.getId());
                if (attempt == 2) throw new BusinessException("操作冲突，请重试");
            }
        }

        log.info("增加商家余额成功 tenantId={}, amount={}, balance={}, orderNo={}",
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

        MerchantBalance balance = getMerchantBalance(tenantId);
        if (balance == null || balance.getBalance().compareTo(amount) < 0) {
            throw new BusinessException("商家余额不足");
        }

        // 扣减余额（乐观锁重试）
        for (int attempt = 0; attempt < 3; attempt++) {
            balance.setBalance(balance.getBalance().subtract(amount));
            balance.setUpdateTime(LocalDateTime.now());
            if (merchantBalanceMapper.updateById(balance) > 0) break;
            balance = merchantBalanceMapper.selectById(balance.getId());
            if (balance == null || balance.getBalance().compareTo(amount) < 0) {
                throw new BusinessException("商家余额不足");
            }
            if (attempt == 2) throw new BusinessException("操作冲突，请重试");
        }

        log.info("扣减商家余额成功 tenantId={}, amount={}, balance={}",
                tenantId, amount, balance.getBalance());
    }

    @Override
    public Page<WithdrawalVO> listWithdrawalsForAdmin(Integer current, Integer size,
                                                      String merchantName, Integer status, String startDate, String endDate) {
        Page<Withdrawal> page = new Page<>(current, size);
        LambdaQueryWrapper<Withdrawal> wrapper = new LambdaQueryWrapper<Withdrawal>()
                .eq(status != null, Withdrawal::getStatus, status)
                .ge(startDate != null, Withdrawal::getApplyTime, startDate)
                .le(endDate != null, Withdrawal::getApplyTime, endDate)
                .eq(Withdrawal::getDeleted, 0)
                .orderByDesc(Withdrawal::getApplyTime);

        if (merchantName != null && !merchantName.isBlank()) {
            List<Long> matchedTenantIds = tenantMapper.selectList(new LambdaQueryWrapper<Tenant>()
                            .eq(Tenant::getDeleted, 0)
                            .like(Tenant::getName, merchantName)
                            .select(Tenant::getId))
                    .stream()
                    .map(Tenant::getId)
                    .collect(Collectors.toList());
            if (matchedTenantIds.isEmpty()) {
                Page<WithdrawalVO> emptyPage = new Page<>(current, size, 0);
                emptyPage.setRecords(Collections.emptyList());
                return emptyPage;
            }
            wrapper.in(Withdrawal::getTenantId, matchedTenantIds);
        }

        Page<Withdrawal> withdrawalPage = withdrawalMapper.selectPage(page, wrapper);
        Page<WithdrawalVO> voPage = new Page<>(withdrawalPage.getCurrent(), withdrawalPage.getSize(), withdrawalPage.getTotal());

        Set<Long> tenantIds = withdrawalPage.getRecords().stream()
                .map(Withdrawal::getTenantId)
                .filter(id -> id != null && id > 0)
                .collect(Collectors.toSet());
        Map<Long, String> tenantNameMap = tenantIds.isEmpty()
                ? Collections.emptyMap()
                : tenantMapper.selectBatchIds(tenantIds).stream()
                .collect(Collectors.toMap(Tenant::getId, Tenant::getName, (left, right) -> left));

        Set<Long> approverIds = withdrawalPage.getRecords().stream()
                .map(Withdrawal::getApproverId)
                .filter(id -> id != null && id > 0)
                .collect(Collectors.toSet());
        Map<Long, String> approverNameMap = approverIds.isEmpty()
                ? Collections.emptyMap()
                : userMapper.selectBatchIds(approverIds).stream()
                .collect(Collectors.toMap(User::getId, User::getUsername, (left, right) -> left));

        List<WithdrawalVO> voList = withdrawalPage.getRecords().stream()
                .map(withdrawal -> {
                    WithdrawalVO vo = new WithdrawalVO();
                    BeanUtils.copyProperties(withdrawal, vo);
                    vo.setMerchantName(tenantNameMap.get(withdrawal.getTenantId()));
                    vo.setApproverName(approverNameMap.get(withdrawal.getApproverId()));
                    return vo;
                })
                .collect(Collectors.toList());
        voPage.setRecords(voList);
        return voPage;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void approveWithdrawal(Long withdrawalId) {
        Withdrawal withdrawal = getPendingWithdrawal(withdrawalId);
        requireFrozenBalance(withdrawal, "冻结余额不足，无法通过提现申请");
        claimWithdrawalStatus(withdrawal, 1, null, null);
        moveFrozenBalanceToWithdrawal(withdrawal);

        log.info("提现审核通过 withdrawalId={}, tenantId={}, amount={}",
                withdrawalId, withdrawal.getTenantId(), withdrawal.getAmount());

        notifyMerchantEmployee(withdrawal.getTenantId(), "提现审批通过",
                "您的提现申请 ¥" + withdrawal.getAmount() + " 已审批通过，资金将尽快到账", "PAYMENT");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void rejectWithdrawal(Long withdrawalId, String reason) {
        if (reason == null || reason.trim().isEmpty()) {
            throw new BusinessException("拒绝原因不能为空");
        }

        Withdrawal withdrawal = getPendingWithdrawal(withdrawalId);
        requireFrozenBalance(withdrawal, "冻结余额不足，无法拒绝提现申请");
        claimWithdrawalStatus(withdrawal, 2, null, reason);
        unfreezeWithdrawalBalance(withdrawal);

        log.info("提现审核拒绝 withdrawalId={}, tenantId={}, reason={}",
                withdrawalId, withdrawal.getTenantId(), reason);

        notifyMerchantEmployee(withdrawal.getTenantId(), "提现审批被拒绝",
                "您的提现申请 ¥" + withdrawal.getAmount() + " 被拒绝，原因：" + reason, "PAYMENT");
    }

    private Withdrawal getPendingWithdrawal(Long withdrawalId) {
        Withdrawal withdrawal = withdrawalMapper.selectOne(new LambdaQueryWrapper<Withdrawal>()
                .eq(Withdrawal::getId, withdrawalId)
                .eq(Withdrawal::getDeleted, 0));
        if (withdrawal == null) {
            throw new BusinessException("提现申请不存在");
        }
        if (withdrawal.getStatus() != 0) {
            throw new BusinessException("提现申请已审核");
        }
        return withdrawal;
    }

    private void claimWithdrawalStatus(Withdrawal withdrawal, Integer status, Long approverId, String rejectReason) {
        com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper<Withdrawal> wrapper =
                new com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper<Withdrawal>()
                        .eq("id", withdrawal.getId())
                        .eq("deleted", 0)
                        .eq("status", 0)
                        .set("status", status)
                        .set("approver_id", approverId)
                        .set("approve_time", LocalDateTime.now())
                        .set("reject_reason", rejectReason);
        if (withdrawalMapper.update(null, wrapper) == 0) {
            throw new BusinessException("提现申请已审核");
        }
    }

    private void moveFrozenBalanceToWithdrawal(Withdrawal withdrawal) {
        MerchantBalance balance = requireFrozenBalance(withdrawal, "冻结余额不足，无法通过提现申请");
        for (int attempt = 0; attempt < 3; attempt++) {
            balance.setFrozenBalance(balance.getFrozenBalance().subtract(withdrawal.getAmount()));
            balance.setTotalWithdrawal(balance.getTotalWithdrawal().add(withdrawal.getAmount()));
            balance.setUpdateTime(LocalDateTime.now());
            if (merchantBalanceMapper.updateById(balance) > 0) {
                return;
            }
            balance = requireFrozenBalance(withdrawal, "冻结余额不足，无法通过提现申请");
        }
        throw new BusinessException("操作冲突，请重试");
    }

    private void unfreezeWithdrawalBalance(Withdrawal withdrawal) {
        MerchantBalance balance = requireFrozenBalance(withdrawal, "冻结余额不足，无法拒绝提现申请");
        for (int attempt = 0; attempt < 3; attempt++) {
            balance.setFrozenBalance(balance.getFrozenBalance().subtract(withdrawal.getAmount()));
            balance.setBalance(balance.getBalance().add(withdrawal.getAmount()));
            balance.setUpdateTime(LocalDateTime.now());
            if (merchantBalanceMapper.updateById(balance) > 0) {
                return;
            }
            balance = requireFrozenBalance(withdrawal, "冻结余额不足，无法拒绝提现申请");
        }
        throw new BusinessException("拒绝提现解冻余额失败，请重试");
    }

    private MerchantBalance requireFrozenBalance(Withdrawal withdrawal, String message) {
        MerchantBalance balance = getMerchantBalance(withdrawal.getTenantId());
        if (balance == null || balance.getFrozenBalance().compareTo(withdrawal.getAmount()) < 0) {
            throw new BusinessException(message);
        }
        return balance;
    }

    /**
     * 向商户的管理员员工发送通知（Withdrawal 无 platformUserId）。
     * 策略：优先找角色为管理员的启用员工，退而求其次取最早加入的启用员工。
     */
    private void notifyMerchantEmployee(Long tenantId, String title, String content, String category) {
        try {
            // 优先：角色为管理员的启用员工
            com.payment.entity.TenantEmployee employee = findAdminEmployee(tenantId);
            if (employee == null) {
                // 兜底：最早加入的启用员工
                employee = tenantEmployeeMapper.selectOne(
                        new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<com.payment.entity.TenantEmployee>()
                                .eq(com.payment.entity.TenantEmployee::getTenantId, tenantId)
                                .eq(com.payment.entity.TenantEmployee::getStatus, 1)
                                .orderByAsc(com.payment.entity.TenantEmployee::getCreateTime)
                                .last("LIMIT 1"));
            }
            if (employee != null && employee.getPlatformUserId() != null) {
                notificationService.send(employee.getPlatformUserId(), title, content, category);
            } else {
                log.debug("商户无可用员工可通知, tenantId={}", tenantId);
            }
        } catch (Exception e) {
            log.warn("发送提现通知失败, tenantId={}", tenantId, e);
        }
    }

    /**
     * 查找商户下角色为管理员的启用员工。
     */
    private com.payment.entity.TenantEmployee findAdminEmployee(Long tenantId) {
        return tenantEmployeeMapper.selectOne(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<com.payment.entity.TenantEmployee>()
                        .eq(com.payment.entity.TenantEmployee::getTenantId, tenantId)
                        .eq(com.payment.entity.TenantEmployee::getStatus, 1)
                        .like(com.payment.entity.TenantEmployee::getEmployeeRole, "admin")
                        .last("LIMIT 1"));
    }
}
