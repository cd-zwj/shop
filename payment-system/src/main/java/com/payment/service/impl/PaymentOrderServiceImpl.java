package com.payment.service.impl;

import com.payment.util.JsonUtils;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.payment.common.BusinessException;
import com.payment.dto.CreateOrderDTO;
import com.payment.dto.PayResponseDTO;
import com.payment.entity.PaymentOrder;
import com.payment.entity.PaymentRecord;
import com.payment.mapper.PaymentOrderMapper;
import com.payment.mapper.PaymentRecordMapper;
import com.payment.service.PaymentOrderService;
import com.payment.service.PaymentService;
import com.payment.service.PointsService;
import com.payment.util.RedisUtils;
import com.payment.util.TenantContextHolder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
public class PaymentOrderServiceImpl extends ServiceImpl<PaymentOrderMapper, PaymentOrder> implements PaymentOrderService {

    @Autowired
    private RedisUtils redisUtils;

    @Autowired
    private PaymentRecordMapper paymentRecordMapper;

    @Autowired
    private PaymentService paymentService;

    @Autowired
    private PointsService pointsService;

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Autowired(required = false)
    private com.payment.service.RechargeService rechargeService;

    @Autowired(required = false)
    private com.payment.service.WithdrawalService withdrawalService;

    private String generateMessageId() {
        String dateStr = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String key = "msg_id:" + dateStr;
        long sequence = redisUtils.incrementAndGet(key, 25, TimeUnit.HOURS);
        return dateStr + String.format("%08d", sequence);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public PaymentOrder createOrder(Long userId, CreateOrderDTO dto) {
        Long tenantId = TenantContextHolder.getTenantId();

        BigDecimal orderAmount = dto.getAmount();
        BigDecimal balanceAmount = BigDecimal.ZERO;
        BigDecimal wechatAmount = dto.getAmount();
        String payType = dto.getPayType();

        if (Boolean.TRUE.equals(dto.getUseBalance()) && rechargeService != null) {
            BigDecimal userBalance = rechargeService.getUserBalance(userId, tenantId);
            if (userBalance.compareTo(BigDecimal.ZERO) > 0) {
                balanceAmount = userBalance.compareTo(orderAmount) >= 0 ? orderAmount : userBalance;
                wechatAmount = orderAmount.subtract(balanceAmount);
                payType = wechatAmount.compareTo(BigDecimal.ZERO) == 0 ? "BALANCE" : "MIXED";
                log.info("订单使用余额支付，userId={}, orderAmount={}, balanceAmount={}, wechatAmount={}",
                        userId, orderAmount, balanceAmount, wechatAmount);
            }
        }

        String orderNo = generateOrderNo();
        PaymentOrder order = new PaymentOrder();
        order.setOrderNo(orderNo);
        order.setTenantId(tenantId);
        order.setUserId(userId);
        order.setAmount(wechatAmount);
        order.setPayType(payType);
        order.setSubject(dto.getSubject());
        order.setBody(dto.getBody());
        order.setOrderStatus("PENDING");
        order.setNotifyUrl(dto.getNotifyUrl());
        order.setExpireTime(LocalDateTime.now().plusMinutes(30));
        save(order);

        if (balanceAmount.compareTo(BigDecimal.ZERO) > 0 && rechargeService != null) {
            try {
                rechargeService.payWithBalance(userId, tenantId, orderNo, balanceAmount);
                log.info("订单余额支付成功，orderNo={}, balanceAmount={}", orderNo, balanceAmount);
            } catch (Exception e) {
                log.error("订单余额支付失败，orderNo={}", orderNo, e);
                throw new BusinessException("余额支付失败，请稍后重试");
            }
        }

        Map<String, Object> messageMap = new HashMap<>();
        messageMap.put("messageId", generateMessageId());
        messageMap.put("orderNo", orderNo);
        rabbitTemplate.convertAndSend("payment.order.created", JsonUtils.toJson(messageMap));
        return order;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public PayResponseDTO pay(Long userId, String orderNo, String tradeType) {
        PaymentOrder order = getOrderByNo(orderNo);
        if (order == null) {
            throw new BusinessException("订单不存在");
        }
        if (!order.getUserId().equals(userId)) {
            throw new BusinessException("无权操作该订单");
        }
        if ("SUCCESS".equals(order.getPayStatus())) {
            throw new BusinessException("订单已支付");
        }

        order.setTradeType(tradeType != null ? tradeType : "NATIVE");

        if ("BALANCE".equals(order.getPayType())) {
            if (rechargeService == null) {
                throw new BusinessException("余额支付服务不可用");
            }
            rechargeService.payWithBalance(userId, order.getTenantId(), orderNo, order.getAmount());
            order.setOrderStatus("PAID");
            order.setPayStatus("SUCCESS");
            order.setPayTime(LocalDateTime.now());
            updateById(order);

            try {
                Integer points = pointsService.calculatePoints(order.getAmount(), order.getTenantId());
                if (points > 0) {
                    pointsService.grantPoints(order.getUserId(), points, "订单支付", orderNo);
                }
            } catch (Exception e) {
                log.error("发放积分失败，orderNo={}", orderNo, e);
            }

            try {
                if (withdrawalService != null) {
                    withdrawalService.addMerchantBalance(order.getTenantId(), order.getAmount(), orderNo);
                }
            } catch (Exception e) {
                log.error("增加商家余额失败，orderNo={}", orderNo, e);
            }

            PayResponseDTO response = new PayResponseDTO();
            response.setOrderNo(orderNo);
            response.setPayType("BALANCE");
            response.setAmount(order.getAmount());
            return response;
        }

        return paymentService.createPay(order);
    }

    @Override
    public PaymentOrder getOrderByNo(String orderNo) {
        return getOne(new LambdaQueryWrapper<PaymentOrder>()
                .eq(PaymentOrder::getOrderNo, orderNo)
                .eq(PaymentOrder::getDeleted, 0));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void cancelOrder(String orderNo) {
        PaymentOrder order = getOrderByNo(orderNo);
        if (order != null && "PENDING".equals(order.getOrderStatus())) {
            order.setOrderStatus("CANCELLED");
            updateById(order);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void handlePayNotify(String payType, Map<String, String> params) {
        log.info("收到支付回调通知，支付方式：{}，参数：{}", payType, params);

        String orderNo = params.get("orderNo") != null ? params.get("orderNo") : params.get("out_trade_no");
        PaymentOrder order = getOrderByNo(orderNo);
        if (order == null) {
            log.error("订单不存在：{}", orderNo);
            return;
        }

        if (!paymentService.verifyNotify(payType, params)) {
            log.error("签名验证失败，orderNo={}", orderNo);
            return;
        }

        order.setOrderStatus("PAID");
        order.setPayStatus("SUCCESS");
        order.setPayTime(LocalDateTime.now());
        order.setThirdPartyOrderNo(params.get("transaction_id") != null ? params.get("transaction_id") : params.get("trade_no"));
        updateById(order);

        PaymentRecord record = new PaymentRecord();
        record.setTenantId(order.getTenantId());
        record.setOrderId(order.getId());
        record.setOrderNo(order.getOrderNo());
        record.setPayType(payType);
        record.setAmount(order.getAmount());
        record.setThirdPartyOrderNo(order.getThirdPartyOrderNo());
        record.setTransactionId(order.getThirdPartyOrderNo());
        record.setPayStatus("SUCCESS");
        record.setNotifyData(params.toString());
        record.setPayTime(LocalDateTime.now());
        record.setNotifyTime(LocalDateTime.now());
        paymentRecordMapper.insert(record);

        Map<String, Object> messageMap = new HashMap<>();
        messageMap.put("messageId", generateMessageId());
        messageMap.put("orderNo", orderNo);
        rabbitTemplate.convertAndSend("payment.order.paid", JsonUtils.toJson(messageMap));

        if (orderNo.startsWith("R")) {
            try {
                if (rechargeService != null) {
                    rechargeService.handleRechargeCallback(orderNo);
                }
            } catch (Exception e) {
                log.error("处理充值订单回调失败：orderNo={}", orderNo, e);
            }
        } else {
            try {
                Integer points = pointsService.calculatePoints(order.getAmount(), order.getTenantId());
                if (points > 0) {
                    pointsService.grantPoints(order.getUserId(), points, "订单支付", orderNo);
                }
            } catch (Exception e) {
                log.error("发放积分失败，orderNo={}", orderNo, e);
            }

            try {
                if (withdrawalService != null) {
                    withdrawalService.addMerchantBalance(order.getTenantId(), order.getAmount(), orderNo);
                }
            } catch (Exception e) {
                log.error("增加商家余额失败，orderNo={}", orderNo, e);
            }
        }

        log.info("订单支付成功，orderNo={}", orderNo);
    }

    private String generateOrderNo() {
        return "PAY" + System.currentTimeMillis() + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }

    @Override
    public com.baomidou.mybatisplus.core.metadata.IPage<PaymentOrder> listUserOrders(
            Long userId,
            com.baomidou.mybatisplus.extension.plugins.pagination.Page<PaymentOrder> page,
            String orderStatus) {
        LambdaQueryWrapper<PaymentOrder> wrapper = new LambdaQueryWrapper<PaymentOrder>()
                .eq(PaymentOrder::getUserId, userId)
                .eq(PaymentOrder::getDeleted, 0)
                .orderByDesc(PaymentOrder::getCreateTime);

        if (orderStatus != null && !orderStatus.isEmpty()) {
            wrapper.eq(PaymentOrder::getOrderStatus, orderStatus);
        }
        return page(page, wrapper);
    }
}


