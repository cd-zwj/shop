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
import com.payment.service.MessageIdempotentService;
import com.payment.service.ProductInventoryService;
import com.payment.service.UserNotificationService;
import com.payment.service.WalletRechargeService;
import com.payment.service.WithdrawalService;
import com.payment.service.delivery.OrderDeliveryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * v1 支付异步消费者
 * <p>
 * 充值到账和订单支付成功都从这里进入，避免在回调线程里堆业务逻辑。
 * 处理流程包括：充值到账处理、订单支付成功后扣库存/更新状态/商户入账/积分发放/会员升级/发送通知/投递交付事件。
 * 通过 {@link MessageIdempotentService} 保障消息幂等性。
 * </p>
 *
 * @author payment-system
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentV1Consumer {

    /** 钱包充值服务 */
    private final WalletRechargeService walletRechargeService;
    /** 销售订单 Mapper */
    private final SalesOrderMapper salesOrderMapper;
    /** 销售订单明细 Mapper */
    private final SalesOrderItemMapper salesOrderItemMapper;
    /** 商品库存服务 */
    private final ProductInventoryService productInventoryService;
    /** 提现/商户余额服务 */
    private final WithdrawalService withdrawalService;
    /** 会员积分账户服务 */
    private final MemberPointsAccountService memberPointsAccountService;
    /** 积分规则 Mapper */
    private final PointsRuleMapper pointsRuleMapper;
    /** 会员服务 */
    private final MemberService memberService;
    /** 用户通知服务 */
    private final UserNotificationService notificationService;
    /** 消息幂等服务 */
    private final MessageIdempotentService messageIdempotentService;
    /** 订单交付服务 */
    private final OrderDeliveryService orderDeliveryService;

    /**
     * 处理充值成功消息
     * <p>
     * 从 {@link RabbitMQConfig#V1_RECHARGE_SUCCESS_QUEUE} 队列中消费充值成功事件，
     * 调用钱包充值服务完成入账操作。
     * </p>
     *
     * @param body 消息体 JSON 字符串，必须包含 bizNo（充值订单编号）
     */
    @RabbitListener(queues = RabbitMQConfig.V1_RECHARGE_SUCCESS_QUEUE)
    public void handleRechargeSuccess(String body) {
        Map<String, Object> payload = JsonUtils.fromJson(body, new TypeReference<Map<String, Object>>() {
        });
        String rechargeNo = String.valueOf(payload.get("bizNo"));
        String messageId = RabbitMQConfig.V1_RECHARGE_SUCCESS_QUEUE + ":" + rechargeNo;
        if (messageIdempotentService.isProcessed(messageId, RabbitMQConfig.V1_RECHARGE_SUCCESS_QUEUE)) {
            log.info("充值成功消息已处理，跳过 messageId={}", messageId);
            return;
        }

        try {
            walletRechargeService.handleRechargeSuccess(rechargeNo);
            messageIdempotentService.recordSuccess(
                    messageId,
                    RabbitMQConfig.V1_RECHARGE_SUCCESS_QUEUE,
                    body,
                    PaymentV1Consumer.class.getSimpleName());
        } catch (Exception e) {
            messageIdempotentService.recordFailure(
                    messageId,
                    RabbitMQConfig.V1_RECHARGE_SUCCESS_QUEUE,
                    body,
                    PaymentV1Consumer.class.getSimpleName(),
                    e.getMessage());
            throw e;
        }
    }

    /**
     * 处理订单支付成功消息
     * <p>
     * 从 {@link RabbitMQConfig#V1_ORDER_PAID_QUEUE} 队列中消费订单支付成功事件，
     * 在事务中完成扣库存、更新订单状态、商户余额入账、积分发放、会员升级检查、
     * 通知发送和交付事件投递等操作。
     * </p>
     *
     * @param body 消息体 JSON 字符串，必须包含 bizNo（销售订单编号）
     */
    @RabbitListener(queues = RabbitMQConfig.V1_ORDER_PAID_QUEUE)
    public void handleOrderPaid(String body) {
        Map<String, Object> payload = JsonUtils.fromJson(body, new TypeReference<Map<String, Object>>() {
        });
        String orderNo = String.valueOf(payload.get("bizNo"));
        String messageId = RabbitMQConfig.V1_ORDER_PAID_QUEUE + ":" + orderNo;
        if (messageIdempotentService.isProcessed(messageId, RabbitMQConfig.V1_ORDER_PAID_QUEUE)) {
            log.info("订单支付成功消息已处理，跳过 messageId={}", messageId);
            return;
        }

        try {
            processOrderPaid(orderNo);
            messageIdempotentService.recordSuccess(
                    messageId,
                    RabbitMQConfig.V1_ORDER_PAID_QUEUE,
                    body,
                    PaymentV1Consumer.class.getSimpleName());
        } catch (Exception e) {
            messageIdempotentService.recordFailure(
                    messageId,
                    RabbitMQConfig.V1_ORDER_PAID_QUEUE,
                    body,
                    PaymentV1Consumer.class.getSimpleName(),
                    e.getMessage());
            throw e;
        }
    }

    /**
     * 执行订单支付成功的完整业务处理流程
     * <p>
     * 处理步骤：
     * <ol>
     *   <li>校验订单存在性和当前状态</li>
     *   <li>扣减商品库存</li>
     *   <li>更新订单支付状态和订单状态为已支付</li>
     *   <li>商户财务余额入账（扣除商户钱包抵扣部分）</li>
     *   <li>按积分规则发放消费积分</li>
     *   <li>检查并自动升级会员等级</li>
     *   <li>发送订单支付成功通知</li>
     *   <li>投递交付事件到交付队列</li>
     * </ol>
     * </p>
     *
     * @param orderNo 销售订单编号
     */
    @Transactional(rollbackFor = Exception.class)
    public void processOrderPaid(String orderNo) {
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

        // 先扣库存，再落已支付状态，避免出现"订单已支付但库存完全没动"的长期不一致。
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
        // 商户结算应以订单实际应付金额为基数，避免优惠券/积分/折扣订单按原价多入账。
        BigDecimal payableAmount = resolvePayableAmount(salesOrder);
        BigDecimal merchantWalletDeductAmount = amountOrZero(salesOrder.getMerchantWalletDeductAmount());
        BigDecimal settlementAmount = payableAmount.subtract(merchantWalletDeductAmount);
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

        // 投递交付事件,由 OrderDeliveryConsumer 按 item 的 productType 分发到对应策略。
        // 入队失败必须抛出 —— 这是支付与交付的原子性保证:
        // 整个 processOrderPaid 处于事务中,Outbox 写失败会回滚订单状态,
        // 由 MQ 重投 + 幂等(根据 messageId)兜底再次进入此方法。
        // 静默 catch 会导致"订单已 PAID 但交付事件丢失",从而漏发商品。
        orderDeliveryService.enqueueDelivery(orderNo);
    }

    private BigDecimal resolvePayableAmount(SalesOrder salesOrder) {
        if (salesOrder.getPayableAmount() != null) {
            return salesOrder.getPayableAmount();
        }
        return amountOrZero(salesOrder.getTotalAmount())
                .subtract(amountOrZero(salesOrder.getDiscountAmount()))
                .subtract(amountOrZero(salesOrder.getPointsDeductAmount()));
    }

    private BigDecimal amountOrZero(BigDecimal amount) {
        return amount == null ? BigDecimal.ZERO : amount;
    }
}
