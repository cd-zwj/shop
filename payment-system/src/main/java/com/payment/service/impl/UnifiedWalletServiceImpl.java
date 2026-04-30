package com.payment.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
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
import java.time.LocalDateTime;
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
            return vo;
        }).collect(Collectors.toList()));
        return result;
    }

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

            account.setAvailableAmount(balanceAfter);
            account.setTotalRecharge(account.getTotalRecharge().add(amount));
            account.setUpdateTime(LocalDateTime.now());

            int updatedRows = accountMapper.updateById(account);
            if (updatedRows == 1) {
                insertLog(platformUserId, amount, bizType, bizNo, remark, balanceBefore, balanceAfter);
                return;
            }
        }

        throw new BusinessException("统一钱包入账失败，请稍后重试");
    }

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

            account.setAvailableAmount(balanceAfter);
            account.setTotalConsume(account.getTotalConsume().add(amount));
            account.setUpdateTime(LocalDateTime.now());

            int updatedRows = accountMapper.updateById(account);
            if (updatedRows == 1) {
                insertLog(platformUserId, amount.negate(), bizType, bizNo, remark, balanceBefore, balanceAfter);
                return;
            }
        }

        throw new BusinessException("统一钱包扣款失败，请稍后重试");
    }

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
