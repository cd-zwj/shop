package com.payment.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.payment.common.BusinessException;
import com.payment.dto.WalletAccountVO;
import com.payment.dto.WalletLogVO;
import com.payment.entity.MerchantWalletAccount;
import com.payment.entity.MerchantWalletLog;
import com.payment.mapper.MerchantWalletAccountMapper;
import com.payment.mapper.MerchantWalletLogMapper;
import com.payment.service.MerchantWalletService;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.stream.Collectors;

/**
 * 商户钱包服务。
 *
 * 商户钱包余额只能在对应商户下消费，同时需要保证并发扣款安全。
 */
@Service
@RequiredArgsConstructor
public class MerchantWalletServiceImpl implements MerchantWalletService {

    private static final int MAX_RETRY_TIMES = 3;

    private final MerchantWalletAccountMapper accountMapper;
    private final MerchantWalletLogMapper logMapper;

    @Override
    public WalletAccountVO getWallet(Long tenantId, Long platformUserId) {
        MerchantWalletAccount account = getOrCreateAccount(tenantId, platformUserId);

        WalletAccountVO vo = new WalletAccountVO();
        vo.setWalletType("MERCHANT");
        vo.setTenantId(tenantId);
        vo.setAvailableAmount(account.getAvailableAmount());
        vo.setFrozenAmount(account.getFrozenAmount());
        vo.setTotalRecharge(account.getTotalRecharge());
        vo.setTotalConsume(account.getTotalConsume());
        return vo;
    }

    @Override
    public Page<WalletLogVO> listLogs(Long tenantId, Long platformUserId, Integer current, Integer size) {
        Page<MerchantWalletLog> entityPage = new Page<>(current, size);
        Page<MerchantWalletLog> page = logMapper.selectPage(entityPage, new LambdaQueryWrapper<MerchantWalletLog>()
                .eq(MerchantWalletLog::getTenantId, tenantId)
                .eq(MerchantWalletLog::getPlatformUserId, platformUserId)
                .orderByDesc(MerchantWalletLog::getCreateTime));

        Page<WalletLogVO> result = new Page<>(page.getCurrent(), page.getSize(), page.getTotal());
        result.setRecords(page.getRecords().stream().map(log -> {
            WalletLogVO vo = new WalletLogVO();
            vo.setWalletType("MERCHANT");
            vo.setTenantId(log.getTenantId());
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
    public void credit(Long tenantId, Long platformUserId, BigDecimal amount, String bizType, String bizNo, String remark) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException("商户钱包入账金额必须大于0");
        }

        for (int retry = 0; retry < MAX_RETRY_TIMES; retry++) {
            MerchantWalletAccount account = getOrCreateAccount(tenantId, platformUserId);
            BigDecimal balanceBefore = account.getAvailableAmount();
            BigDecimal balanceAfter = balanceBefore.add(amount);

            account.setAvailableAmount(balanceAfter);
            account.setTotalRecharge(account.getTotalRecharge().add(amount));
            account.setUpdateTime(LocalDateTime.now());

            int updatedRows = accountMapper.updateById(account);
            if (updatedRows == 1) {
                insertLog(tenantId, platformUserId, amount, bizType, bizNo, remark, balanceBefore, balanceAfter);
                return;
            }
        }

        throw new BusinessException("商户钱包入账失败，请稍后重试");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void debit(Long tenantId, Long platformUserId, BigDecimal amount, String bizType, String bizNo, String remark) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException("商户钱包扣减金额必须大于0");
        }

        for (int retry = 0; retry < MAX_RETRY_TIMES; retry++) {
            MerchantWalletAccount account = getOrCreateAccount(tenantId, platformUserId);
            if (account.getAvailableAmount().compareTo(amount) < 0) {
                throw new BusinessException("商户钱包余额不足");
            }

            BigDecimal balanceBefore = account.getAvailableAmount();
            BigDecimal balanceAfter = balanceBefore.subtract(amount);

            account.setAvailableAmount(balanceAfter);
            account.setTotalConsume(account.getTotalConsume().add(amount));
            account.setUpdateTime(LocalDateTime.now());

            int updatedRows = accountMapper.updateById(account);
            if (updatedRows == 1) {
                insertLog(tenantId, platformUserId, amount.negate(), bizType, bizNo, remark, balanceBefore, balanceAfter);
                return;
            }
        }

        throw new BusinessException("商户钱包扣款失败，请稍后重试");
    }

    private MerchantWalletAccount getOrCreateAccount(Long tenantId, Long platformUserId) {
        MerchantWalletAccount account = accountMapper.selectOne(new LambdaQueryWrapper<MerchantWalletAccount>()
                .eq(MerchantWalletAccount::getTenantId, tenantId)
                .eq(MerchantWalletAccount::getPlatformUserId, platformUserId));
        if (account != null) {
            return account;
        }

        MerchantWalletAccount newAccount = new MerchantWalletAccount();
        newAccount.setTenantId(tenantId);
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
            MerchantWalletAccount existingAccount = accountMapper.selectOne(new LambdaQueryWrapper<MerchantWalletAccount>()
                    .eq(MerchantWalletAccount::getTenantId, tenantId)
                    .eq(MerchantWalletAccount::getPlatformUserId, platformUserId));
            if (existingAccount != null) {
                return existingAccount;
            }
            throw duplicateKeyException;
        }
    }

    private void insertLog(Long tenantId,
                           Long platformUserId,
                           BigDecimal changeAmount,
                           String bizType,
                           String bizNo,
                           String remark,
                           BigDecimal balanceBefore,
                           BigDecimal balanceAfter) {
        MerchantWalletLog walletLog = new MerchantWalletLog();
        walletLog.setTenantId(tenantId);
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
