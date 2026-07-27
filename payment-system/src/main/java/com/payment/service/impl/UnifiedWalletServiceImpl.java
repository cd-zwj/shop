package com.payment.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.payment.common.BusinessException;
import com.payment.dto.WalletAccountVO;
import com.payment.dto.WalletLogVO;
import com.payment.entity.UnifiedWalletAccount;
import com.payment.entity.UnifiedWalletLog;
import com.payment.mapper.UnifiedWalletAccountMapper;
import com.payment.mapper.UnifiedWalletLogMapper;
import com.payment.service.UnifiedWalletService;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.stream.Collectors;

/**
 * 统一钱包服务。
 *
 * 这里对账户更新采用乐观锁 + 重试，避免并发充值/扣款时出现丢更新或超扣。
 */
@Service
@RequiredArgsConstructor
public class UnifiedWalletServiceImpl implements UnifiedWalletService {

    private static final int MAX_RETRY_TIMES = 3;

    private final UnifiedWalletAccountMapper accountMapper;
    private final UnifiedWalletLogMapper logMapper;

    /** 查询用户统一钱包余额信息，账户不存在时自动创建。 */
    @Override
    public WalletAccountVO getWallet(Long platformUserId) {
        UnifiedWalletAccount account = getOrCreateAccount(platformUserId);

        WalletAccountVO vo = new WalletAccountVO();
        vo.setWalletType("UNIFIED");
        vo.setAvailableAmount(account.getAvailableAmount());
        vo.setFrozenAmount(account.getFrozenAmount());
        vo.setTotalRecharge(account.getTotalRecharge());
        vo.setTotalConsume(account.getTotalConsume());
        return vo;
    }

    /**
     * 分页查询用户统一钱包的资金变动流水。
     *
     * @param platformUserId 平台用户 ID
     * @param current        当前页码
     * @param size           每页数量
     * @return 分页流水记录
     */
    @Override
    public Page<WalletLogVO> listLogs(Long platformUserId, Integer current, Integer size) {
        Page<UnifiedWalletLog> entityPage = new Page<>(current, size);
        Page<UnifiedWalletLog> page = logMapper.selectPage(entityPage, new LambdaQueryWrapper<UnifiedWalletLog>()
                .eq(UnifiedWalletLog::getPlatformUserId, platformUserId)
                .orderByDesc(UnifiedWalletLog::getCreateTime));

        Page<WalletLogVO> result = new Page<>(page.getCurrent(), page.getSize(), page.getTotal());
        result.setRecords(page.getRecords().stream().map(log -> {
            WalletLogVO vo = new WalletLogVO();
            vo.setWalletType("UNIFIED");
            vo.setBizType(log.getBizType());
            vo.setBizNo(log.getBizNo());
            vo.setChangeAmount(log.getChangeAmount());
            vo.setBalanceBefore(log.getBalanceBefore());
            vo.setBalanceAfter(log.getBalanceAfter());
            vo.setRemark(log.getRemark());
            vo.setCreateTime(log.getCreateTime());
            vo.attachTrace();
            return vo;
        }).collect(Collectors.toList()));
        return result;
    }

    /**
     * 统一钱包入账（充值、退款等场景）。
     * <p>
     * 采用乐观锁 + 重试机制（最多 3 次）保障并发安全，
     * 成功后同步写入资金变动流水记录。
     *
     * @param platformUserId 平台用户 ID
     * @param amount         入账金额，必须大于 0
     * @param bizType        业务类型
     * @param bizNo          业务单号
     * @param remark         备注说明
     * @throws BusinessException 金额不合法或重试耗尽时抛出
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void credit(Long platformUserId, BigDecimal amount, String bizType, String bizNo, String remark) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException("统一钱包入账金额必须大于0");
        }

        for (int retry = 0; retry < MAX_RETRY_TIMES; retry++) {
            UnifiedWalletAccount account = getOrCreateAccount(platformUserId);
            BigDecimal balanceBefore = account.getAvailableAmount();
            BigDecimal balanceAfter = balanceBefore.add(amount);

            LambdaUpdateWrapper<UnifiedWalletAccount> update = baseAccountUpdate(account)
                    .setSql("available_amount = COALESCE(available_amount, 0) + " + moneyLiteral(amount));
            if ("UNIFIED_RECHARGE".equals(bizType)) {
                update.setSql("total_recharge = COALESCE(total_recharge, 0) + " + moneyLiteral(amount));
            }
            int updatedRows = accountMapper.update(null, update.setSql("update_time = NOW()"));
            if (updatedRows == 1) {
                insertLog(platformUserId, amount, bizType, bizNo, remark, balanceBefore, balanceAfter);
                return;
            }
        }

        throw new BusinessException("统一钱包入账失败，请稍后重试");
    }

    /**
     * 统一钱包扣款（消费场景）。
     * <p>
     * 采用乐观锁 + 重试机制（最多 3 次）保障并发安全，
     * 扣款前校验可用余额是否充足，成功后同步写入资金变动流水。
     *
     * @param platformUserId 平台用户 ID
     * @param amount         扣减金额，必须大于 0
     * @param bizType        业务类型
     * @param bizNo          业务单号
     * @param remark         备注说明
     * @throws BusinessException 余额不足、金额不合法或重试耗尽时抛出
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void debit(Long platformUserId, BigDecimal amount, String bizType, String bizNo, String remark) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException("统一钱包扣减金额必须大于0");
        }

        for (int retry = 0; retry < MAX_RETRY_TIMES; retry++) {
            UnifiedWalletAccount account = getOrCreateAccount(platformUserId);
            if (account.getAvailableAmount().compareTo(amount) < 0) {
                throw new BusinessException("统一钱包余额不足");
            }

            BigDecimal balanceBefore = account.getAvailableAmount();
            BigDecimal balanceAfter = balanceBefore.subtract(amount);

            int updatedRows = accountMapper.update(null, baseAccountUpdate(account)
                    .ge(UnifiedWalletAccount::getAvailableAmount, amount)
                    .setSql("available_amount = available_amount - " + moneyLiteral(amount))
                    .setSql("total_consume = COALESCE(total_consume, 0) + " + moneyLiteral(amount))
                    .setSql("update_time = NOW()"));
            if (updatedRows == 1) {
                insertLog(platformUserId, amount.negate(), bizType, bizNo, remark, balanceBefore, balanceAfter);
                return;
            }
        }

        throw new BusinessException("统一钱包扣款失败，请稍后重试");
    }

    /**
     * 获取或创建用户的钱包账户。
     * <p>
     * 账户不存在时自动创建初始账户，并发创建时通过 DuplicateKeyException 兜底。
     */
    private UnifiedWalletAccount getOrCreateAccount(Long platformUserId) {
        UnifiedWalletAccount account = accountMapper.selectOne(new LambdaQueryWrapper<UnifiedWalletAccount>()
                .eq(UnifiedWalletAccount::getPlatformUserId, platformUserId));
        if (account != null) {
            return account;
        }

        UnifiedWalletAccount newAccount = new UnifiedWalletAccount();
        newAccount.setPlatformUserId(platformUserId);
        newAccount.setAvailableAmount(BigDecimal.ZERO);
        newAccount.setFrozenAmount(BigDecimal.ZERO);
        newAccount.setTotalRecharge(BigDecimal.ZERO);
        newAccount.setTotalConsume(BigDecimal.ZERO);
        newAccount.setVersion(0);
        newAccount.setStatus(1);

        try {
            accountMapper.insert(newAccount);
            return newAccount;
        } catch (DuplicateKeyException duplicateKeyException) {
            UnifiedWalletAccount existingAccount = accountMapper.selectOne(new LambdaQueryWrapper<UnifiedWalletAccount>()
                    .eq(UnifiedWalletAccount::getPlatformUserId, platformUserId));
            if (existingAccount != null) {
                return existingAccount;
            }
            throw duplicateKeyException;
        }
    }

    /**
     * 余额更新必须用数据库端原子表达式，避免乐观锁失败后复用已变更实体造成重复加减。
     */
    private LambdaUpdateWrapper<UnifiedWalletAccount> baseAccountUpdate(UnifiedWalletAccount account) {
        LambdaUpdateWrapper<UnifiedWalletAccount> wrapper = new LambdaUpdateWrapper<UnifiedWalletAccount>()
                .eq(UnifiedWalletAccount::getId, account.getId())
                .eq(UnifiedWalletAccount::getPlatformUserId, account.getPlatformUserId())
                .setSql("version = COALESCE(version, 0) + 1");
        if (account.getVersion() == null) {
            return wrapper.isNull(UnifiedWalletAccount::getVersion);
        }
        return wrapper.eq(UnifiedWalletAccount::getVersion, account.getVersion());
    }

    private String moneyLiteral(BigDecimal amount) {
        return amount.stripTrailingZeros().toPlainString();
    }

    /** 插入统一钱包资金变动流水记录。 */
    private void insertLog(Long platformUserId,
                           BigDecimal changeAmount,
                           String bizType,
                           String bizNo,
                           String remark,
                           BigDecimal balanceBefore,
                           BigDecimal balanceAfter) {
        UnifiedWalletLog walletLog = new UnifiedWalletLog();
        walletLog.setPlatformUserId(platformUserId);
        walletLog.setBizType(bizType);
        walletLog.setBizNo(bizNo);
        walletLog.setChangeAmount(changeAmount);
        walletLog.setBalanceBefore(balanceBefore);
        walletLog.setBalanceAfter(balanceAfter);
        walletLog.setRemark(remark);
        logMapper.insert(walletLog);
    }
}
