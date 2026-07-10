package com.payment.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
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

/**
 * 提现服务实现类，管理商户提现与余额的完整生命周期。
 * <p>
 * 核心职责：
 * <ul>
 *   <li><b>提现申请</b>：商户发起提现，冻结对应余额（乐观锁重试）</li>
 *   <li><b>提现审核</b>：平台管理员审批通过或拒绝，通过时将冻结余额转为已提现，拒绝时解冻</li>
 *   <li><b>余额管理</b>：增加/扣减商户余额，支持乐观锁并发安全</li>
 *   <li><b>查询</b>：商户端提现列表、管理员端提现列表（含商户名称和审核人信息）</li>
 * </ul>
 * <p>
 * 余额操作采用乐观锁 + 3 次重试机制，防止并发冲突。
 * 提现审核通过后向商户管理员发送通知。
 *
 * @see com.payment.service.WithdrawalService
 */
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

    /**
     * 创建提现申请。
     * <p>
     * 校验余额充足后，将对应金额从可用余额冻结到冻结余额（乐观锁 3 次重试），
     * 创建提现记录，初始状态为待审核（status=0）。
     *
     * @param tenantId 商户 ID
     * @param dto      提现申请参数（含金额等）
     * @return 已创建的提现记录
     * @throws BusinessException 余额不足或乐观锁冲突时抛出
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Withdrawal createWithdrawal(Long tenantId, WithdrawalApplyDTO dto) {
        if (tenantId == null) {
            throw new BusinessException("租户信息不存在");
        }

        BigDecimal amount = dto.getAmount();
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException("提现金额必须大于0");
        }

        // 冻结余额（CAS 条件更新）：balance -> frozenBalance
        for (int attempt = 0; attempt < 3; attempt++) {
            MerchantBalance balance = getMerchantBalance(tenantId);
            if (balance == null || balance.getBalance().compareTo(amount) < 0) {
                throw new BusinessException("可提现余额不足");
            }
            if (freezeAvailableBalance(balance, amount)) {
                break;
            }
            if (attempt == 2) {
                throw new BusinessException("操作冲突，请重试");
            }
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
                tenantId, amount, withdrawal.getId());
        return withdrawal;
    }

    /**
     * 分页查询商户提现记录列表。
     *
     * @param query 查询条件（含租户 ID、状态、分页参数）
     * @return 提现记录分页结果，按申请时间降序
     */
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

    /**
     * 审核提现申请（带审核人信息）。
     * <p>
     * 通过时将冻结余额转为已提现总额；拒绝时解冻冻结余额。使用 CAS 抢占式更新状态，
     * 防止重复审核。
     *
     * @param approverId 审核人 ID
     * @param dto        审核参数（含提现 ID、是否通过、拒绝原因）
     * @throws BusinessException 提现不存在、已审核或余额不足时抛出
     */
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

    /**
     * 查询商户余额信息。
     *
     * @param tenantId 商户 ID
     * @return 商户余额实体，不存在时返回 null
     */
    @Override
    public MerchantBalance getMerchantBalance(Long tenantId) {
        return merchantBalanceMapper.selectOne(new LambdaQueryWrapper<MerchantBalance>()
                .eq(MerchantBalance::getTenantId, tenantId)
                .eq(MerchantBalance::getDeleted, 0));
    }

    /**
     * 增加商户余额。
     * <p>
     * 用于订单支付成功后的商户入账。若商户余额记录不存在则自动创建，
     * 存在则乐观锁累加余额和总收入。处理并发创建的 DuplicateKeyException。
     *
     * @param tenantId 商户 ID
     * @param amount   入账金额，必须大于 0
     * @param orderNo  关联订单号（用于日志追踪）
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void addMerchantBalance(Long tenantId, BigDecimal amount, String orderNo) {
        addMerchantBalance(tenantId, amount, orderNo, BigDecimal.ZERO);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void addMerchantBalance(Long tenantId, BigDecimal amount, String orderNo, BigDecimal platformFee) {
        if (tenantId == null) {
            throw new BusinessException("租户信息不存在");
        }
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            log.warn("增加商家余额金额无效 tenantId={}, amount={}", tenantId, amount);
            return;
        }
        BigDecimal safePlatformFee = platformFee == null ? BigDecimal.ZERO : platformFee;
        if (safePlatformFee.compareTo(BigDecimal.ZERO) < 0) {
            throw new BusinessException("平台服务费不能为负数");
        }

        MerchantBalance balance = getMerchantBalance(tenantId);
        if (balance == null) {
            balance = new MerchantBalance();
            balance.setTenantId(tenantId);
            balance.setBalance(amount);
            balance.setFrozenBalance(BigDecimal.ZERO);
            balance.setTotalIncome(amount);
            balance.setTotalWithdrawal(BigDecimal.ZERO);
            balance.setTotalPlatformFee(safePlatformFee);
            balance.setDeleted(0);
            balance.setCreateTime(LocalDateTime.now());
            balance.setUpdateTime(LocalDateTime.now());
            try {
                merchantBalanceMapper.insert(balance);
            } catch (Exception e) {
                // 并发创建 → DuplicateKeyException → 回退到重试更新
                log.warn("商家余额并发创建冲突，转为更新，tenantId={}", tenantId);
                retryIncreaseMerchantBalance(tenantId, amount, safePlatformFee);
            }
        } else {
            retryIncreaseMerchantBalance(tenantId, amount, safePlatformFee);
        }

        log.info("增加商家余额成功 tenantId={}, amount={}, platformFee={}, orderNo={}",
                tenantId, amount, safePlatformFee, orderNo);
    }

    /**
     * 扣减商户余额。
     * <p>
     * 用于退款等场景下的余额扣减，乐观锁 3 次重试。
     *
     * @param tenantId 商户 ID
     * @param amount   扣减金额，必须大于 0
     * @throws BusinessException 余额不足或乐观锁冲突时抛出
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deductMerchantBalance(Long tenantId, BigDecimal amount) {
        if (tenantId == null) {
            throw new BusinessException("租户信息不存在");
        }
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException("扣减金额必须大于0");
        }

        // 扣减余额（CAS 条件更新）
        for (int attempt = 0; attempt < 3; attempt++) {
            MerchantBalance balance = getMerchantBalance(tenantId);
            if (balance == null || balance.getBalance().compareTo(amount) < 0) {
                throw new BusinessException("商家余额不足");
            }
            if (deductAvailableBalance(balance, amount)) {
                break;
            }
            if (attempt == 2) {
                throw new BusinessException("操作冲突，请重试");
            }
        }

        log.info("扣减商家余额成功 tenantId={}, amount={}", tenantId, amount);
    }

    /**
     * 管理员端分页查询提现列表。
     * <p>
     * 支持按商户名称模糊搜索、状态筛选、日期范围筛选。
     * 自动关联商户名称和审核人名称。
     *
     * @param current      当前页码
     * @param size         每页条数
     * @param merchantName 商户名称模糊搜索，可为 null
     * @param status       提现状态筛选，可为 null
     * @param startDate    开始日期，可为 null
     * @param endDate      结束日期，可为 null
     * @return 包含商户名称和审核人名称的提现 VO 分页结果
     */
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

    /**
     * 通过提现申请（简化版，无审核人信息）。
     * <p>
     * CAS 抢占式更新状态为已通过，将冻结余额转为已提现总额，并通知商户。
     *
     * @param withdrawalId 提现申请 ID
     * @throws BusinessException 提现不存在、已审核或余额不足时抛出
     */
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

    /**
     * 拒绝提现申请。
     * <p>
     * CAS 抢占式更新状态为已拒绝，解冻冻结余额恢复到可用余额，并通知商户。
     *
     * @param withdrawalId 提现申请 ID
     * @param reason       拒绝原因，不能为空
     * @throws BusinessException 拒绝原因为空、提现不存在或已审核时抛出
     */
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

    /**
     * 获取待审核状态的提现申请。
     *
     * @param withdrawalId 提现申请 ID
     * @return 提现申请实体
     * @throws BusinessException 提现不存在或已审核时抛出
     */
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

    /**
     * CAS 抢占式更新提现状态。
     * <p>
     * 仅当当前状态为待审核（status=0）时才能更新成功，防止并发重复审核。
     *
     * @param withdrawal   提现申请
     * @param status       目标状态（1=通过，2=拒绝）
     * @param approverId   审核人 ID，可为 null
     * @param rejectReason 拒绝原因，通过时为 null
     * @throws BusinessException 已审核时抛出
     */
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

    /**
     * 将冻结余额转为已提现总额（审核通过时调用）。
     *
     * @param withdrawal 已通过的提现申请
     */
    private void moveFrozenBalanceToWithdrawal(Withdrawal withdrawal) {
        for (int attempt = 0; attempt < 3; attempt++) {
            MerchantBalance balance = requireFrozenBalance(withdrawal, "冻结余额不足，无法通过提现申请");
            if (moveFrozenToWithdrawal(balance, withdrawal.getAmount())) {
                return;
            }
        }
        throw new BusinessException("操作冲突，请重试");
    }

    /**
     * 解冻冻结余额，恢复到可用余额（审核拒绝时调用）。
     *
     * @param withdrawal 已拒绝的提现申请
     */
    private void unfreezeWithdrawalBalance(Withdrawal withdrawal) {
        for (int attempt = 0; attempt < 3; attempt++) {
            MerchantBalance balance = requireFrozenBalance(withdrawal, "冻结余额不足，无法拒绝提现申请");
            if (unfreezeBalance(balance, withdrawal.getAmount())) {
                return;
            }
        }
        throw new BusinessException("拒绝提现解冻余额失败，请重试");
    }

    private void retryIncreaseMerchantBalance(Long tenantId, BigDecimal amount, BigDecimal platformFee) {
        for (int attempt = 0; attempt < 3; attempt++) {
            MerchantBalance balance = getMerchantBalance(tenantId);
            if (balance == null) {
                throw new BusinessException("创建商家余额失败");
            }
            if (increaseAvailableBalance(balance, amount, platformFee)) {
                return;
            }
        }
        throw new BusinessException("操作冲突，请重试");
    }

    private boolean freezeAvailableBalance(MerchantBalance balance, BigDecimal amount) {
        UpdateWrapper<MerchantBalance> wrapper = baseBalanceUpdate(balance)
                .ge("balance", amount)
                .setSql("balance = balance - " + moneyLiteral(amount))
                .setSql("frozen_balance = COALESCE(frozen_balance, 0) + " + moneyLiteral(amount));
        return merchantBalanceMapper.update(null, wrapper) > 0;
    }

    private boolean increaseAvailableBalance(MerchantBalance balance, BigDecimal amount, BigDecimal platformFee) {
        UpdateWrapper<MerchantBalance> wrapper = baseBalanceUpdate(balance)
                .setSql("balance = COALESCE(balance, 0) + " + moneyLiteral(amount))
                .setSql("total_income = COALESCE(total_income, 0) + " + moneyLiteral(amount))
                .setSql("total_platform_fee = COALESCE(total_platform_fee, 0) + " + moneyLiteral(platformFee));
        return merchantBalanceMapper.update(null, wrapper) > 0;
    }

    private boolean deductAvailableBalance(MerchantBalance balance, BigDecimal amount) {
        UpdateWrapper<MerchantBalance> wrapper = baseBalanceUpdate(balance)
                .ge("balance", amount)
                .setSql("balance = balance - " + moneyLiteral(amount));
        return merchantBalanceMapper.update(null, wrapper) > 0;
    }

    private boolean moveFrozenToWithdrawal(MerchantBalance balance, BigDecimal amount) {
        UpdateWrapper<MerchantBalance> wrapper = baseBalanceUpdate(balance)
                .ge("frozen_balance", amount)
                .setSql("frozen_balance = frozen_balance - " + moneyLiteral(amount))
                .setSql("total_withdrawal = COALESCE(total_withdrawal, 0) + " + moneyLiteral(amount));
        return merchantBalanceMapper.update(null, wrapper) > 0;
    }

    private boolean unfreezeBalance(MerchantBalance balance, BigDecimal amount) {
        UpdateWrapper<MerchantBalance> wrapper = baseBalanceUpdate(balance)
                .ge("frozen_balance", amount)
                .setSql("frozen_balance = frozen_balance - " + moneyLiteral(amount))
                .setSql("balance = COALESCE(balance, 0) + " + moneyLiteral(amount));
        return merchantBalanceMapper.update(null, wrapper) > 0;
    }

    private UpdateWrapper<MerchantBalance> baseBalanceUpdate(MerchantBalance balance) {
        UpdateWrapper<MerchantBalance> wrapper = new UpdateWrapper<MerchantBalance>()
                .eq("id", balance.getId())
                .eq("tenant_id", balance.getTenantId())
                .eq("deleted", 0)
                .set("update_time", LocalDateTime.now())
                .setSql("version = COALESCE(version, 0) + 1");
        if (balance.getVersion() == null) {
            return wrapper.isNull("version");
        }
        return wrapper.eq("version", balance.getVersion());
    }

    private String moneyLiteral(BigDecimal amount) {
        return amount.stripTrailingZeros().toPlainString();
    }

    /**
     * 校验冻结余额是否满足提现金额。
     *
     * @param withdrawal 提现申请
     * @param message    余额不足时的错误信息
     * @return 商户余额实体
     * @throws BusinessException 余额不足时抛出
     */
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
