package com.payment.consumer;

import com.payment.util.JsonUtils;
import com.fasterxml.jackson.core.type.TypeReference;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.payment.config.RabbitMQConfig;
import com.payment.entity.PointsRule;
import com.payment.entity.SalesOrder;
import com.payment.entity.SalesOrderItem;
import com.payment.enums.OrderStatusEnum;
import com.payment.enums.PayStatusEnum;
import com.payment.mapper.PointsRuleMapper;
import com.payment.mapper.SalesOrderItemMapper;
import com.payment.mapper.SalesOrderMapper;
import com.payment.service.MemberPointsAccountService;
import com.payment.service.MemberService;
import com.payment.service.ProductInventoryService;
import com.payment.service.UserNotificationService;
import com.payment.service.WalletRechargeService;
import com.payment.service.WithdrawalService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * v1 支付异步消费者。
 *
 * 充值到账和订单支付成功都从这里进入，避免在回调线程里堆业务逻辑。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentV1Consumer {

    private final WalletRechargeService walletRechargeService;
    private final SalesOrderMapper salesOrderMapper;
    private final SalesOrderItemMapper salesOrderItemMapper;
    private final ProductInventoryService productInventoryService;
    private final WithdrawalService withdrawalService;
    private final MemberPointsAccountService memberPointsAccountService;
    private final PointsRuleMapper pointsRuleMapper;
    private final MemberService memberService;
    private final UserNotificationService notificationService;

    @RabbitListener(queues = RabbitMQConfig.V1_RECHARGE_SUCCESS_QUEUE)
    public void handleRechargeSuccess(String body) {
        Map<String, Object> payload = JsonUtils.fromJson(body, new TypeReference<Map<String, Object>>() {
        });
        String rechargeNo = String.valueOf(payload.get("bizNo"));
        walletRechargeService.handleRechargeSuccess(rechargeNo);
    }

    @RabbitListener(queues = RabbitMQConfig.V1_ORDER_PAID_QUEUE)
    @Transactional(rollbackFor = Exception.class)
    public void handleOrderPaid(String body) {
        Map<String, Object> payload = JsonUtils.fromJson(body, new TypeReference<Map<String, Object>>() {
        });
        String orderNo = String.valueOf(payload.get("bizNo"));

        SalesOrder salesOrder = salesOrderMapper.selectOne(new LambdaQueryWrapper<SalesOrder>()
                .eq(SalesOrder::getOrderNo, orderNo)
                .eq(SalesOrder::getDeleted, 0));
        if (salesOrder == null) {
            log.warn("订单支付成功消息对应订单不存在, orderNo={}", orderNo);
            return;
        }
        if (PayStatusEnum.SUCCESS.name().equals(salesOrder.getPayStatus())
                && OrderStatusEnum.PAID.name().equals(salesOrder.getOrderStatus())) {
            return;
        }

        // 先扣库存，再落已支付状态，避免出现“订单已支付但库存完全没动”的长期不一致。
        List<SalesOrderItem> orderItems = salesOrderItemMapper.selectByOrderId(salesOrder.getId());
        for (SalesOrderItem orderItem : orderItems) {
            productInventoryService.deductStock(
                    salesOrder.getTenantId(),
                    orderItem.getProductId(),
                    orderItem.getQuantity(),
                    salesOrder.getOrderNo()
            );
        }

        salesOrder.setPayStatus(PayStatusEnum.SUCCESS.name());
        salesOrder.setOrderStatus(OrderStatusEnum.PAID.name());
        salesOrderMapper.updateById(salesOrder);

        // 商户钱包部分在充值成功时已经进入商户财务余额，订单消费时不重复入账。
        BigDecimal settlementAmount = salesOrder.getTotalAmount().subtract(salesOrder.getMerchantWalletDeductAmount());
        if (settlementAmount.compareTo(BigDecimal.ZERO) > 0) {
            withdrawalService.addMerchantBalance(salesOrder.getTenantId(), settlementAmount, orderNo);
        }

        PointsRule pointsRule = pointsRuleMapper.selectOne(new LambdaQueryWrapper<PointsRule>()
                .eq(PointsRule::getTenantId, salesOrder.getTenantId())
                .eq(PointsRule::getStatus, 1));
        if (pointsRule != null && pointsRule.getPointsAmount() != null && pointsRule.getPointsAmount() > 0) {
            int points = pointsRule.getPointsAmount();
            memberPointsAccountService.grantPoints(
                    salesOrder.getTenantId(),
                    salesOrder.getPlatformUserId(),
                    points,
                    "SALES_ORDER",
                    orderNo,
                    "消费赠送积分"
            );
        }

        // 订单支付成功后自动检查会员等级升级
        try {
            memberService.checkAndAutoUpgrade(salesOrder.getTenantId(), salesOrder.getPlatformUserId());
        } catch (Exception e) {
            log.warn("会员自动升级检查失败, orderNo={}, userId={}", orderNo, salesOrder.getPlatformUserId(), e);
        }

        // 通知用户：订单支付成功
        try {
            notificationService.send(
                    salesOrder.getPlatformUserId(),
                    "订单支付成功",
                    "您的订单 " + orderNo + " 已支付成功，金额 ¥" + salesOrder.getTotalAmount(),
                    "ORDER");
        } catch (Exception e) {
            log.warn("发送订单支付成功通知失败, orderNo={}", orderNo, e);
        }
    }
}


