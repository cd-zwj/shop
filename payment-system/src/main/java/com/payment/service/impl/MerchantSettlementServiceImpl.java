package com.payment.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.payment.entity.MerchantBalance;
import com.payment.entity.MerchantWalletLog;
import com.payment.mapper.MerchantBalanceMapper;
import com.payment.mapper.MerchantWalletLogMapper;
import com.payment.util.JsonUtils;
import com.payment.service.MerchantSettlementService;
import com.payment.service.TenantConfigService;
import com.payment.service.WithdrawalService;
import com.payment.vo.TenantConfigVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;

/**
 * 商户订单结算服务实现。
 * <p>
 * 平台服务费率从 tenant_config 的 PLATFORM_FEE_RATE 键读取（数字、0-100 百分比，默认 0），
 * 结算时先按费率计算平台抽成，再把净额记入商户余额并在收款流水中记录平台服务费。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MerchantSettlementServiceImpl implements MerchantSettlementService {

    /** tenant_config 中存储平台服务费率的键名，值为 0-100 的百分比数字。 */
    public static final String PLATFORM_FEE_RATE_KEY = "PLATFORM_FEE_RATE";

    private static final BigDecimal ONE_HUNDRED = new BigDecimal("100");

    private final TenantConfigService tenantConfigService;
    private final WithdrawalService withdrawalService;
    private final MerchantBalanceMapper merchantBalanceMapper;
    private final MerchantWalletLogMapper merchantWalletLogMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public BigDecimal settleOrder(Long tenantId, BigDecimal settlementAmount, String orderNo) {
        if (settlementAmount == null || settlementAmount.compareTo(BigDecimal.ZERO) <= 0) {
            log.warn("商户结算金额无效 tenantId={}, amount={}, orderNo={}", tenantId, settlementAmount, orderNo);
            return BigDecimal.ZERO;
        }

        BigDecimal feeRate = getFeeRatePercent(tenantId);
        BigDecimal fee = settlementAmount.multiply(feeRate)
                .divide(ONE_HUNDRED, 2, RoundingMode.HALF_UP);
        BigDecimal netAmount = settlementAmount.subtract(fee);

        BigDecimal balanceBefore = currentAvailableBalance(tenantId);
        withdrawalService.addMerchantBalance(tenantId, netAmount, orderNo, fee);
        BigDecimal balanceAfter = currentAvailableBalance(tenantId);
        insertSettlementLog(tenantId, orderNo, netAmount, fee, balanceBefore, balanceAfter);

        // 服务费抽成的明细日志保留在此处，方便排查结算异常；流水由 addMerchantBalance 统一处理。
        if (fee.compareTo(BigDecimal.ZERO) > 0) {
            log.info("平台服务费抽成 tenantId={}, orderNo={}, settlement={}, fee={}, net={}",
                    tenantId, orderNo, settlementAmount, fee, netAmount);
        }

        return netAmount;
    }

    private BigDecimal currentAvailableBalance(Long tenantId) {
        MerchantBalance balance = merchantBalanceMapper.selectOne(new LambdaQueryWrapper<MerchantBalance>()
                .eq(MerchantBalance::getTenantId, tenantId)
                .eq(MerchantBalance::getDeleted, 0));
        if (balance == null || balance.getBalance() == null) {
            return BigDecimal.ZERO;
        }
        return balance.getBalance();
    }

    private void insertSettlementLog(Long tenantId,
                                     String orderNo,
                                     BigDecimal netAmount,
                                     BigDecimal fee,
                                     BigDecimal balanceBefore,
                                     BigDecimal balanceAfter) {
        MerchantWalletLog walletLog = new MerchantWalletLog();
        walletLog.setTenantId(tenantId);
        walletLog.setBizType("PAYMENT");
        walletLog.setBizNo(orderNo);
        walletLog.setChangeAmount(netAmount);
        walletLog.setFeeAmount(fee);
        walletLog.setBalanceBefore(balanceBefore);
        walletLog.setBalanceAfter(balanceAfter);
        walletLog.setRemark(fee.compareTo(BigDecimal.ZERO) > 0
                ? "订单结算入账，已扣平台服务费 " + fee
                : "订单结算入账");
        walletLog.setCreateTime(LocalDateTime.now());
        merchantWalletLogMapper.insert(walletLog);
    }

    @Override
    public BigDecimal getFeeRatePercent(Long tenantId) {
        try {
            TenantConfigVO config = tenantConfigService.getByKey(tenantId, PLATFORM_FEE_RATE_KEY);
            return parseRate(config == null ? null : config.getConfigValue());
        } catch (Exception e) {
            log.warn("读取平台服务费率失败,使用默认 0 tenantId={}", tenantId, e);
            return BigDecimal.ZERO;
        }
    }

    /**
     * 解析 fee rate 字符串为 0-100 的 BigDecimal。
     * 接受纯数字、{"rate": 3} JSON 与无效值，无效或越界统一返回 0。
     */
    private BigDecimal parseRate(String raw) {
        if (raw == null || raw.isBlank()) {
            return BigDecimal.ZERO;
        }
        String trimmed = raw.trim();
        try {
            BigDecimal value;
            if (trimmed.startsWith("{") || trimmed.startsWith("[")) {
                JsonNode node = JsonUtils.fromJsonTree(trimmed);
                JsonNode rateNode = node.path("rate");
                if (rateNode.isMissingNode() || !rateNode.canConvertToInt()) {
                    return BigDecimal.ZERO;
                }
                value = new BigDecimal(rateNode.asText());
            } else {
                value = new BigDecimal(trimmed);
            }
            if (value.compareTo(BigDecimal.ZERO) < 0 || value.compareTo(ONE_HUNDRED) > 0) {
                log.warn("平台服务费率越界, 使用 0, value={}", value);
                return BigDecimal.ZERO;
            }
            return value;
        } catch (Exception e) {
            log.warn("平台服务费率解析失败, 使用 0, raw={}", raw, e);
            return BigDecimal.ZERO;
        }
    }
}
