package com.payment.service.impl;

import com.alibaba.fastjson2.JSON;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.payment.common.BusinessException;
import com.payment.config.RabbitMQConfig;
import com.payment.dto.AppCreateOrderDTO;
import com.payment.dto.OrderPaymentVO;
import com.payment.dto.PayResponseDTO;
import com.payment.dto.WalletAccountVO;
import com.payment.entity.PaymentBill;
import com.payment.entity.PointsRule;
import com.payment.entity.SalesOrder;
import com.payment.entity.TenantMember;
import com.payment.enums.OrderStatusEnum;
import com.payment.enums.PayStatusEnum;
import com.payment.enums.PaymentBizTypeEnum;
import com.payment.enums.WalletStrategyEnum;
import com.payment.mapper.SalesOrderMapper;
import com.payment.mapper.TenantMemberMapper;
import com.payment.service.AppOrderService;
import com.payment.service.MemberPointsAccountService;
import com.payment.service.MerchantWalletService;
import com.payment.service.PaymentBillV1Service;
import com.payment.service.UnifiedWalletService;
import com.payment.service.WithdrawalService;
import com.payment.util.BizNoGenerator;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;

/**
 * 用户端订单服务。
 *
 * 这里按用户选择的钱包策略拆分扣款，并只为外部支付部分创建支付单。
 */
@Service
@RequiredArgsConstructor
public class AppOrderServiceImpl implements AppOrderService {

    private final SalesOrderMapper salesOrderMapper;
    private final TenantMemberMapper tenantMemberMapper;
    private final UnifiedWalletService unifiedWalletService;
    private final MerchantWalletService merchantWalletService;
    private final PaymentBillV1Service paymentBillV1Service;
    private final RabbitTemplate rabbitTemplate;
    private final WithdrawalService withdrawalService;
    private final MemberPointsAccountService memberPointsAccountService;
    private final com.payment.mapper.PointsRuleMapper pointsRuleMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public OrderPaymentVO createOrder(Long platformUserId, AppCreateOrderDTO dto) {
        ensureTenantMember(dto.getTenantId(), platformUserId);

        WalletSplit split = calculateWalletSplit(platformUserId, dto);
        String orderNo = BizNoGenerator.generate("SO");

        SalesOrder salesOrder = new SalesOrder();
        salesOrder.setOrderNo(orderNo);
        salesOrder.setTenantId(dto.getTenantId());
        salesOrder.setPlatformUserId(platformUserId);
        salesOrder.setOrderStatus(OrderStatusEnum.CREATED.name());
        salesOrder.setPayStatus(split.externalPayAmount.compareTo(BigDecimal.ZERO) == 0
                ? PayStatusEnum.SUCCESS.name()
                : PayStatusEnum.WAIT_PAY.name());
        salesOrder.setTotalAmount(dto.getTotalAmount());
        salesOrder.setDiscountAmount(BigDecimal.ZERO);
        salesOrder.setWalletDeductAmount(split.unifiedWalletAmount.add(split.merchantWalletAmount));
        salesOrder.setUnifiedWalletDeductAmount(split.unifiedWalletAmount);
        salesOrder.setMerchantWalletDeductAmount(split.merchantWalletAmount);
        salesOrder.setExternalPayAmount(split.externalPayAmount);
        salesOrder.setPayableAmount(dto.getTotalAmount());
        salesOrder.setSubject(dto.getSubject());
        salesOrder.setSource(dto.getSource());
        salesOrder.setWalletStrategy(dto.getWalletStrategy().name());
        salesOrder.setExpireTime(LocalDateTime.now().plusMinutes(30));
        salesOrder.setDeleted(0);
        salesOrderMapper.insert(salesOrder);

        // 钱包金额在下单时直接扣减，取消订单时再按原路径回补。
        if (split.unifiedWalletAmount.compareTo(BigDecimal.ZERO) > 0) {
            unifiedWalletService.debit(platformUserId, split.unifiedWalletAmount, "SALES_ORDER", orderNo, "订单消费扣减");
        }
        if (split.merchantWalletAmount.compareTo(BigDecimal.ZERO) > 0) {
            merchantWalletService.debit(dto.getTenantId(), platformUserId, split.merchantWalletAmount, "SALES_ORDER", orderNo, "订单消费扣减");
        }

        OrderPaymentVO result = buildOrderPaymentVO(salesOrder);
        if (split.externalPayAmount.compareTo(BigDecimal.ZERO) > 0) {
            PaymentBill paymentBill = paymentBillV1Service.createBill(
                    PaymentBizTypeEnum.SALES_ORDER.name(),
                    orderNo,
                    dto.getTenantId(),
                    platformUserId,
                    split.externalPayAmount
            );
            PayResponseDTO payResponse = paymentBillV1Service.createExternalPayment(paymentBill);
            result.setPaymentBillNo(paymentBill.getBillNo());
            result.setExternalPayUrl(payResponse.getPayUrl());
        } else {
            salesOrder.setOrderStatus(OrderStatusEnum.PAID.name());
            salesOrder.setPayStatus(PayStatusEnum.SUCCESS.name());
            salesOrderMapper.updateById(salesOrder);
            settlePaidOrder(salesOrder);
            result.setOrderStatus(OrderStatusEnum.PAID.name());
            result.setPayStatus(PayStatusEnum.SUCCESS.name());
        }

        return result;
    }

    @Override
    public Page<SalesOrder> listOrders(Long platformUserId, Integer current, Integer size) {
        return salesOrderMapper.selectPage(new Page<>(current, size), new LambdaQueryWrapper<SalesOrder>()
                .eq(SalesOrder::getPlatformUserId, platformUserId)
                .eq(SalesOrder::getDeleted, 0)
                .orderByDesc(SalesOrder::getCreateTime));
    }

    @Override
    public SalesOrder getByOrderNo(Long platformUserId, String orderNo) {
        SalesOrder salesOrder = salesOrderMapper.selectOne(new LambdaQueryWrapper<SalesOrder>()
                .eq(SalesOrder::getOrderNo, orderNo)
                .eq(SalesOrder::getPlatformUserId, platformUserId)
                .eq(SalesOrder::getDeleted, 0));
        if (salesOrder == null) {
            throw new BusinessException("订单不存在");
        }
        return salesOrder;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void cancelOrder(Long platformUserId, String orderNo) {
        SalesOrder salesOrder = getByOrderNo(platformUserId, orderNo);
        if (OrderStatusEnum.PAID.name().equals(salesOrder.getOrderStatus())) {
            throw new BusinessException("已支付订单不允许取消");
        }
        if (OrderStatusEnum.CANCELLED.name().equals(salesOrder.getOrderStatus())) {
            return;
        }

        if (salesOrder.getUnifiedWalletDeductAmount().compareTo(BigDecimal.ZERO) > 0) {
            unifiedWalletService.credit(platformUserId, salesOrder.getUnifiedWalletDeductAmount(), "ORDER_CANCEL_REFUND", orderNo, "取消订单回退");
        }
        if (salesOrder.getMerchantWalletDeductAmount().compareTo(BigDecimal.ZERO) > 0) {
            merchantWalletService.credit(salesOrder.getTenantId(), platformUserId, salesOrder.getMerchantWalletDeductAmount(), "ORDER_CANCEL_REFUND", orderNo, "取消订单回退");
        }

        salesOrder.setOrderStatus(OrderStatusEnum.CANCELLED.name());
        salesOrder.setPayStatus(PayStatusEnum.CLOSED.name());
        salesOrderMapper.updateById(salesOrder);
    }

    private void ensureTenantMember(Long tenantId, Long platformUserId) {
        TenantMember tenantMember = tenantMemberMapper.selectOne(new LambdaQueryWrapper<TenantMember>()
                .eq(TenantMember::getTenantId, tenantId)
                .eq(TenantMember::getPlatformUserId, platformUserId));
        if (tenantMember != null) {
            return;
        }

        TenantMember newTenantMember = new TenantMember();
        newTenantMember.setTenantId(tenantId);
        newTenantMember.setPlatformUserId(platformUserId);
        newTenantMember.setMemberNo(BizNoGenerator.generate("TM"));
        newTenantMember.setMemberStatus(1);
        newTenantMember.setRegisterSource("APP");
        tenantMemberMapper.insert(newTenantMember);
    }

    private WalletSplit calculateWalletSplit(Long platformUserId, AppCreateOrderDTO dto) {
        BigDecimal totalAmount = dto.getTotalAmount();
        BigDecimal unifiedBalance = unifiedWalletService.getWallet(platformUserId).getAvailableAmount();
        BigDecimal merchantBalance = merchantWalletService.getWallet(dto.getTenantId(), platformUserId).getAvailableAmount();
        boolean allowFallback = Boolean.TRUE.equals(dto.getAllowExternalPayFallback());

        return switch (dto.getWalletStrategy()) {
            case NO_WALLET -> new WalletSplit(BigDecimal.ZERO, BigDecimal.ZERO, totalAmount);
            case UNIFIED_ONLY -> calculateSingleWallet(totalAmount, unifiedBalance, allowFallback, true);
            case MERCHANT_ONLY -> calculateSingleWallet(totalAmount, merchantBalance, allowFallback, false);
            case MERCHANT_THEN_UNIFIED -> calculateChainedWallet(totalAmount, merchantBalance, unifiedBalance, allowFallback, false);
            case UNIFIED_THEN_MERCHANT -> calculateChainedWallet(totalAmount, unifiedBalance, merchantBalance, allowFallback, true);
            case CUSTOM_SPLIT -> calculateCustomSplit(totalAmount, dto, unifiedBalance, merchantBalance, allowFallback);
        };
    }

    private WalletSplit calculateSingleWallet(BigDecimal totalAmount,
                                              BigDecimal balance,
                                              boolean allowFallback,
                                              boolean unifiedFirst) {
        BigDecimal used = balance.min(totalAmount);
        if (!allowFallback && used.compareTo(totalAmount) < 0) {
            throw new BusinessException("钱包余额不足");
        }
        BigDecimal external = totalAmount.subtract(used);
        return unifiedFirst
                ? new WalletSplit(used, BigDecimal.ZERO, external)
                : new WalletSplit(BigDecimal.ZERO, used, external);
    }

    private WalletSplit calculateChainedWallet(BigDecimal totalAmount,
                                               BigDecimal firstBalance,
                                               BigDecimal secondBalance,
                                               boolean allowFallback,
                                               boolean unifiedFirst) {
        BigDecimal firstUsed = firstBalance.min(totalAmount);
        BigDecimal remain = totalAmount.subtract(firstUsed);
        BigDecimal secondUsed = secondBalance.min(remain);
        BigDecimal external = totalAmount.subtract(firstUsed).subtract(secondUsed);
        if (!allowFallback && external.compareTo(BigDecimal.ZERO) > 0) {
            throw new BusinessException("钱包余额不足");
        }
        return unifiedFirst
                ? new WalletSplit(firstUsed, secondUsed, external)
                : new WalletSplit(secondUsed, firstUsed, external);
    }

    private WalletSplit calculateCustomSplit(BigDecimal totalAmount,
                                             AppCreateOrderDTO dto,
                                             BigDecimal unifiedBalance,
                                             BigDecimal merchantBalance,
                                             boolean allowFallback) {
        BigDecimal unifiedAmount = defaultAmount(dto.getUnifiedWalletAmount());
        BigDecimal merchantAmount = defaultAmount(dto.getMerchantWalletAmount());

        if (unifiedAmount.compareTo(unifiedBalance) > 0 || merchantAmount.compareTo(merchantBalance) > 0) {
            throw new BusinessException("自定义钱包金额超过可用余额");
        }

        BigDecimal walletTotal = unifiedAmount.add(merchantAmount);
        if (walletTotal.compareTo(totalAmount) > 0) {
            throw new BusinessException("自定义钱包金额不能超过订单金额");
        }

        BigDecimal external = totalAmount.subtract(walletTotal);
        if (!allowFallback && external.compareTo(BigDecimal.ZERO) > 0) {
            throw new BusinessException("当前订单不允许剩余金额走外部支付");
        }
        return new WalletSplit(unifiedAmount, merchantAmount, external);
    }

    private BigDecimal defaultAmount(BigDecimal amount) {
        return amount == null ? BigDecimal.ZERO : amount;
    }

    private void publishPaidEvent(String orderNo) {
        rabbitTemplate.convertAndSend(RabbitMQConfig.V1_ORDER_PAID_QUEUE, JSON.toJSONString(Map.of(
                "bizType", PaymentBizTypeEnum.SALES_ORDER.name(),
                "bizNo", orderNo
        )));
    }

    /**
     * 纯钱包支付没有外部回调，直接在本地事务里完成结算。
     */
    private void settlePaidOrder(SalesOrder salesOrder) {
        BigDecimal settlementAmount = salesOrder.getTotalAmount().subtract(salesOrder.getMerchantWalletDeductAmount());
        if (settlementAmount.compareTo(BigDecimal.ZERO) > 0) {
            withdrawalService.addMerchantBalance(salesOrder.getTenantId(), settlementAmount, salesOrder.getOrderNo());
        }

        PointsRule pointsRule = pointsRuleMapper.selectOne(new LambdaQueryWrapper<PointsRule>()
                .eq(PointsRule::getTenantId, salesOrder.getTenantId())
                .eq(PointsRule::getDeleted, 0)
                .eq(PointsRule::getEnabled, 1));
        if (pointsRule != null && pointsRule.getPointsRatio() != null && pointsRule.getPointsRatio() > 0) {
            int points = salesOrder.getTotalAmount().intValue() * pointsRule.getPointsRatio();
            memberPointsAccountService.grantPoints(salesOrder.getTenantId(), salesOrder.getPlatformUserId(), points, "SALES_ORDER", salesOrder.getOrderNo(), "消费赠送积分");
        }
    }

    private OrderPaymentVO buildOrderPaymentVO(SalesOrder salesOrder) {
        OrderPaymentVO vo = new OrderPaymentVO();
        vo.setOrderNo(salesOrder.getOrderNo());
        vo.setOrderStatus(salesOrder.getOrderStatus());
        vo.setPayStatus(salesOrder.getPayStatus());
        vo.setTotalAmount(salesOrder.getTotalAmount());
        vo.setUnifiedWalletDeductAmount(salesOrder.getUnifiedWalletDeductAmount());
        vo.setMerchantWalletDeductAmount(salesOrder.getMerchantWalletDeductAmount());
        vo.setExternalPayAmount(salesOrder.getExternalPayAmount());
        return vo;
    }

    private record WalletSplit(BigDecimal unifiedWalletAmount,
                               BigDecimal merchantWalletAmount,
                               BigDecimal externalPayAmount) {
    }
}
