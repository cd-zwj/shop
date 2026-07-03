package com.payment.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.payment.common.BusinessException;
import com.payment.dto.CreateMerchantWalletRechargeDTO;
import com.payment.dto.CreateUnifiedWalletRechargeDTO;
import com.payment.dto.PayResponseDTO;
import com.payment.dto.RechargePaymentVO;
import com.payment.entity.MerchantRechargeRule;
import com.payment.entity.PaymentBill;
import com.payment.entity.RechargeOrderV1;
import com.payment.entity.TenantMember;
import com.payment.enums.PayStatusEnum;
import com.payment.enums.PaymentBizTypeEnum;
import com.payment.enums.PaymentChannelCodeEnum;
import com.payment.enums.WalletTypeEnum;
import com.payment.mapper.MerchantRechargeRuleMapper;
import com.payment.mapper.RechargeOrderV1Mapper;
import com.payment.mapper.TenantMemberMapper;
import com.payment.service.MemberPointsAccountService;
import com.payment.service.MerchantWalletService;
import com.payment.service.PaymentBillV1Service;
import com.payment.service.UnifiedWalletService;
import com.payment.service.WalletRechargeService;
import com.payment.service.WithdrawalService;
import com.payment.util.BizNoGenerator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 钱包充值服务。
 *
 * 充值只负责创建业务单和支付单，到账由支付回调成功后的异步链路处理。
 */
@Service
@RequiredArgsConstructor
public class WalletRechargeServiceImpl implements WalletRechargeService {

    private final RechargeOrderV1Mapper rechargeOrderV1Mapper;
    private final MerchantRechargeRuleMapper merchantRechargeRuleMapper;
    private final TenantMemberMapper tenantMemberMapper;
    private final PaymentBillV1Service paymentBillV1Service;
    private final UnifiedWalletService unifiedWalletService;
    private final MerchantWalletService merchantWalletService;
    private final MemberPointsAccountService memberPointsAccountService;
    private final WithdrawalService withdrawalService;

    /**
     * 创建统一钱包充值订单并发起支付。
     * <p>
     * 流程：创建充值订单 → 创建支付账单 → 调用第三方支付 → 返回支付链接。
     *
     * @param platformUserId 平台用户 ID
     * @param dto            统一钱包充值请求（金额、支付渠道）
     * @return 充值支付信息 VO（含充值单号和外部支付链接）
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public RechargePaymentVO createUnifiedRecharge(Long platformUserId, CreateUnifiedWalletRechargeDTO dto) {
        RechargeOrderV1 rechargeOrder = new RechargeOrderV1();
        rechargeOrder.setRechargeNo(BizNoGenerator.generate("RC"));
        rechargeOrder.setWalletType(WalletTypeEnum.UNIFIED.name());
        rechargeOrder.setPlatformUserId(platformUserId);
        rechargeOrder.setRechargeAmount(dto.getAmount());
        rechargeOrder.setGiftAmount(BigDecimal.ZERO);
        rechargeOrder.setGiftPoints(0);
        rechargeOrder.setActualCreditAmount(dto.getAmount());
        rechargeOrder.setBizStatus(PayStatusEnum.WAIT_PAY.name());
        rechargeOrder.setDeleted(0);
        rechargeOrderV1Mapper.insert(rechargeOrder);

        PaymentBill paymentBill = paymentBillV1Service.createBill(
                PaymentBizTypeEnum.RECHARGE.name(),
                rechargeOrder.getRechargeNo(),
                null,
                platformUserId,
                dto.getAmount(),
                requireChannel(dto.getPaymentChannelCode())
        );
        PayResponseDTO payResponse = paymentBillV1Service.createExternalPayment(paymentBill);
        return buildRechargeVO(rechargeOrder, paymentBill, payResponse);
    }

    /**
     * 创建商户钱包充值订单并发起支付。
     * <p>
     * 流程：校验充值规则 → 确保用户为商户会员 → 创建充值订单 → 创建支付账单 → 调用第三方支付。
     * 充值规则中可包含赠送金额和赠送积分。
     *
     * @param tenantId       租户 ID
     * @param platformUserId 平台用户 ID
     * @param dto            商户钱包充值请求（规则 ID、支付渠道）
     * @return 充值支付信息 VO
     * @throws BusinessException 充值规则不存在时抛出
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public RechargePaymentVO createMerchantRecharge(Long tenantId, Long platformUserId, CreateMerchantWalletRechargeDTO dto) {
        MerchantRechargeRule rule = merchantRechargeRuleMapper.selectOne(new LambdaQueryWrapper<MerchantRechargeRule>()
                .eq(MerchantRechargeRule::getId, dto.getRuleId())
                .eq(MerchantRechargeRule::getTenantId, tenantId)
                .eq(MerchantRechargeRule::getStatus, 1));
        if (rule == null) {
            throw new BusinessException("充值规则不存在");
        }

        ensureTenantMember(tenantId, platformUserId);

        RechargeOrderV1 rechargeOrder = new RechargeOrderV1();
        rechargeOrder.setRechargeNo(BizNoGenerator.generate("RC"));
        rechargeOrder.setWalletType(WalletTypeEnum.MERCHANT.name());
        rechargeOrder.setTenantId(tenantId);
        rechargeOrder.setPlatformUserId(platformUserId);
        rechargeOrder.setRuleId(rule.getId());
        rechargeOrder.setRechargeAmount(rule.getRechargeAmount());
        rechargeOrder.setGiftAmount(rule.getGiftAmount());
        rechargeOrder.setGiftPoints(rule.getGiftPoints());
        rechargeOrder.setActualCreditAmount(rule.getRechargeAmount().add(rule.getGiftAmount()));
        rechargeOrder.setBizStatus(PayStatusEnum.WAIT_PAY.name());
        rechargeOrder.setDeleted(0);
        rechargeOrderV1Mapper.insert(rechargeOrder);

        PaymentBill paymentBill = paymentBillV1Service.createBill(
                PaymentBizTypeEnum.RECHARGE.name(),
                rechargeOrder.getRechargeNo(),
                tenantId,
                platformUserId,
                rule.getRechargeAmount(),
                requireChannel(dto.getPaymentChannelCode())
        );
        PayResponseDTO payResponse = paymentBillV1Service.createExternalPayment(paymentBill);
        return buildRechargeVO(rechargeOrder, paymentBill, payResponse);
    }

    /**
     * 处理充值支付成功回调。
     * <p>
     * 将充值订单标记为成功，并执行以下到账操作：
     * <ul>
     *   <li>统一钱包充值：直接入账统一钱包</li>
     *   <li>商户钱包充值：入账商户钱包 + 发放赠送积分 + 增加商户财务余额</li>
     * </ul>
     *
     * @param rechargeNo 充值单号
     * @throws BusinessException 充值单不存在时抛出
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void handleRechargeSuccess(String rechargeNo) {
        RechargeOrderV1 rechargeOrder = rechargeOrderV1Mapper.selectOne(new LambdaQueryWrapper<RechargeOrderV1>()
                .eq(RechargeOrderV1::getRechargeNo, rechargeNo)
                .eq(RechargeOrderV1::getDeleted, 0));
        if (rechargeOrder == null) {
            throw new BusinessException("充值单不存在");
        }
        if (PayStatusEnum.SUCCESS.name().equals(rechargeOrder.getBizStatus())) {
            return;
        }

        rechargeOrder.setBizStatus(PayStatusEnum.SUCCESS.name());
        rechargeOrder.setUpdateTime(LocalDateTime.now());
        rechargeOrderV1Mapper.updateById(rechargeOrder);

        if (WalletTypeEnum.UNIFIED.name().equals(rechargeOrder.getWalletType())) {
            unifiedWalletService.credit(rechargeOrder.getPlatformUserId(), rechargeOrder.getActualCreditAmount(), "UNIFIED_RECHARGE", rechargeNo, "统一钱包充值");
            return;
        }

        merchantWalletService.credit(rechargeOrder.getTenantId(), rechargeOrder.getPlatformUserId(), rechargeOrder.getActualCreditAmount(), "MERCHANT_RECHARGE", rechargeNo, "商户钱包充值");
        if (rechargeOrder.getGiftPoints() != null && rechargeOrder.getGiftPoints() > 0) {
            memberPointsAccountService.grantPoints(rechargeOrder.getTenantId(), rechargeOrder.getPlatformUserId(), rechargeOrder.getGiftPoints(), "MERCHANT_RECHARGE", rechargeNo, "充值赠送积分");
        }

        // 商户充值得到的真实现金在充值成功时进入商户财务余额。
        withdrawalService.addMerchantBalance(rechargeOrder.getTenantId(), rechargeOrder.getRechargeAmount(), rechargeNo);
    }

    /**
     * 确保用户已是该商户的会员，若不是则自动注册为会员。
     *
     * @param tenantId       租户 ID
     * @param platformUserId 平台用户 ID
     */
    private void ensureTenantMember(Long tenantId, Long platformUserId) {
        TenantMember tenantMember = tenantMemberMapper.selectOne(new LambdaQueryWrapper<TenantMember>()
                .eq(TenantMember::getTenantId, tenantId)
                .eq(TenantMember::getPlatformUserId, platformUserId));
        if (tenantMember != null) {
            return;
        }

        TenantMember newMember = new TenantMember();
        newMember.setTenantId(tenantId);
        newMember.setPlatformUserId(platformUserId);
        newMember.setMemberNo(BizNoGenerator.generate("TM"));
        newMember.setMemberStatus(1);
        newMember.setRegisterSource("APP");
        tenantMemberMapper.insert(newMember);
    }

    /**
     * 将充值订单和支付信息组装为返回 VO。
     *
     * @param rechargeOrder 充值订单实体
     * @param paymentBill   支付账单实体
     * @param payResponse   第三方支付返回信息
     * @return 充值支付信息 VO
     */
    private RechargePaymentVO buildRechargeVO(RechargeOrderV1 rechargeOrder, PaymentBill paymentBill, PayResponseDTO payResponse) {
        RechargePaymentVO vo = new RechargePaymentVO();
        vo.setRechargeNo(rechargeOrder.getRechargeNo());
        vo.setWalletType(rechargeOrder.getWalletType());
        vo.setTenantId(rechargeOrder.getTenantId());
        vo.setRechargeAmount(rechargeOrder.getRechargeAmount());
        vo.setGiftAmount(rechargeOrder.getGiftAmount());
        vo.setGiftPoints(rechargeOrder.getGiftPoints());
        vo.setPaymentBillNo(paymentBill.getBillNo());
        vo.setExternalPayUrl(payResponse.getPayUrl());
        return vo;
    }

    /**
     * 校验支付渠道不为空，为空时抛出异常。
     *
     * @param channelCode 支付渠道枚举
     * @return 校验通过的支付渠道枚举
     * @throws BusinessException 支付渠道为空时抛出
     */
    private PaymentChannelCodeEnum requireChannel(PaymentChannelCodeEnum channelCode) {
        if (channelCode == null) {
            throw new BusinessException("支付渠道不能为空");
        }
        return channelCode;
    }
}
