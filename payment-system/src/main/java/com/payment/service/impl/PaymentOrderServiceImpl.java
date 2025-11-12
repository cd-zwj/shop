package com.payment.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.payment.common.BusinessException;
import com.payment.config.PaymentConfig;
import com.payment.dto.CreateOrderDTO;
import com.payment.dto.PayResponseDTO;
import com.payment.entity.PaymentOrder;
import com.payment.entity.PaymentRecord;
import com.payment.mapper.PaymentOrderMapper;
import com.payment.mapper.PaymentRecordMapper;
import com.payment.service.PaymentOrderService;
import com.payment.service.PaymentService;
import com.payment.service.PointsService;
import com.payment.util.TenantContextHolder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

/**
 * 订单服务实现类
 */
@Slf4j
@Service
public class PaymentOrderServiceImpl extends ServiceImpl<PaymentOrderMapper, PaymentOrder> implements PaymentOrderService {
    
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
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public PaymentOrder createOrder(Long userId, CreateOrderDTO dto) {
        Long tenantId = TenantContextHolder.getTenantId();
        
        // 处理余额支付
        BigDecimal orderAmount = dto.getAmount();
        BigDecimal balanceAmount = BigDecimal.ZERO;
        BigDecimal wechatAmount = dto.getAmount();
        String payType = dto.getPayType();
        
        if (Boolean.TRUE.equals(dto.getUseBalance()) && rechargeService != null) {
            // 查询用户余额
            BigDecimal userBalance = rechargeService.getUserBalance(userId, tenantId);
            
            if (userBalance.compareTo(BigDecimal.ZERO) > 0) {
                // 计算余额支付金额
                balanceAmount = userBalance.compareTo(orderAmount) >= 0 ? orderAmount : userBalance;
                wechatAmount = orderAmount.subtract(balanceAmount);
                
                // 如果余额足够支付全部订单，使用纯余额支付
                if (wechatAmount.compareTo(BigDecimal.ZERO) == 0) {
                    payType = "BALANCE";
                } else {
                    // 否则使用组合支付
                    payType = "MIXED";
                }
                
                log.info("订单使用余额支付：userId={}, orderAmount={}, balanceAmount={}, wechatAmount={}", 
                        userId, orderAmount, balanceAmount, wechatAmount);
            }
        }
        
        // 生成订单号
        String orderNo = generateOrderNo();
        
        // 创建订单
        PaymentOrder order = new PaymentOrder();
        order.setOrderNo(orderNo);
        order.setTenantId(tenantId);
        order.setUserId(userId);
        order.setAmount(wechatAmount); // 实际需要支付的金额（扣除余额后）
        order.setPayType(payType);
        order.setSubject(dto.getSubject());
        order.setBody(dto.getBody());
        order.setOrderStatus("PENDING");
        order.setNotifyUrl(dto.getNotifyUrl());
        order.setExpireTime(LocalDateTime.now().plusMinutes(30)); // 30分钟过期
        
        save(order);
        
        // 如果使用了余额支付，先扣除余额
        if (balanceAmount.compareTo(BigDecimal.ZERO) > 0 && rechargeService != null) {
            try {
                rechargeService.payWithBalance(userId, tenantId, orderNo, balanceAmount);
                log.info("订单余额支付成功：orderNo={}, balanceAmount={}", orderNo, balanceAmount);
            } catch (Exception e) {
                log.error("订单余额支付失败：orderNo={}", orderNo, e);
                throw new BusinessException("余额支付失败：" + e.getMessage());
            }
        }
        
        // 发送订单创建消息到RabbitMQ
        rabbitTemplate.convertAndSend("payment.order.created", orderNo);
        
        return order;
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public PayResponseDTO pay(Long userId, String orderNo) {
        PaymentOrder order = getOrderByNo(orderNo);
        
        if (order == null) {
            throw new BusinessException("订单不存在");
        }
        
        if (!order.getUserId().equals(userId)) {
            throw new BusinessException("无权操作此订单");
        }
        
        if (!"PENDING".equals(order.getOrderStatus())) {
            throw new BusinessException("订单状态不正确");
        }
        
        // 检查是否使用余额支付
        if ("BALANCE".equals(order.getPayType())) {
            // 纯余额支付
            if (rechargeService != null) {
                rechargeService.payWithBalance(userId, order.getTenantId(), orderNo, order.getAmount());
                
                // 更新订单状态为已支付
                order.setOrderStatus("PAID");
                order.setPayStatus("SUCCESS");
                order.setPayTime(LocalDateTime.now());
                updateById(order);
                
                // 发放积分
                try {
                    Integer points = pointsService.calculatePoints(order.getAmount(), order.getTenantId());
                    if (points > 0) {
                        pointsService.grantPoints(order.getUserId(), points, "订单支付", orderNo);
                    }
                } catch (Exception e) {
                    log.error("发放积分失败：orderNo={}", orderNo, e);
                }
                
                // 增加商家余额
                try {
                    if (withdrawalService != null) {
                        withdrawalService.addMerchantBalance(order.getTenantId(), order.getAmount(), orderNo);
                        log.info("订单完成（余额支付），增加商家余额：orderNo={}, tenantId={}, amount={}", 
                                orderNo, order.getTenantId(), order.getAmount());
                    }
                } catch (Exception e) {
                    log.error("增加商家余额失败：orderNo={}", orderNo, e);
                }
                
                PayResponseDTO response = new PayResponseDTO();
                response.setOrderNo(orderNo);
                response.setPayType("BALANCE");
                response.setAmount(order.getAmount());
                return response;
            } else {
                throw new BusinessException("余额支付服务不可用");
            }
        }
        
        // 调用支付服务
        PayResponseDTO payResponse = paymentService.createPay(order);
        
        return payResponse;
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
        
        // 验证签名
        boolean verifyResult = paymentService.verifyNotify(payType, params);
        if (!verifyResult) {
            log.error("签名验证失败：{}", orderNo);
            return;
        }
        
        // 更新订单状态
        order.setOrderStatus("PAID");
        order.setPayStatus("SUCCESS");
        order.setPayTime(LocalDateTime.now());
        order.setThirdPartyOrderNo(params.get("transaction_id") != null ? params.get("transaction_id") : params.get("trade_no"));
        updateById(order);
        
        // 创建支付记录
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
        
        // 发送支付成功消息
        rabbitTemplate.convertAndSend("payment.order.paid", orderNo);
        
        // 判断是否为充值订单
        if (orderNo.startsWith("R")) {
            // 处理充值订单回调
            try {
                if (rechargeService != null) {
                    rechargeService.handleRechargeCallback(orderNo);
                    log.info("充值订单支付成功：{}", orderNo);
                }
            } catch (Exception e) {
                log.error("处理充值订单回调失败：orderNo={}", orderNo, e);
            }
        } else {
            // 普通订单处理
            // 1. 发放积分
            try {
                Integer points = pointsService.calculatePoints(order.getAmount(), order.getTenantId());
                if (points > 0) {
                    pointsService.grantPoints(order.getUserId(), points, "订单支付", orderNo);
                    log.info("订单支付成功，发放积分：orderNo={}, userId={}, points={}", 
                            orderNo, order.getUserId(), points);
                }
            } catch (Exception e) {
                log.error("发放积分失败：orderNo={}", orderNo, e);
                // 积分发放失败不影响订单支付流程
            }
            
            // 2. 增加商家余额
            try {
                if (withdrawalService != null) {
                    withdrawalService.addMerchantBalance(order.getTenantId(), order.getAmount(), orderNo);
                    log.info("订单完成，增加商家余额：orderNo={}, tenantId={}, amount={}", 
                            orderNo, order.getTenantId(), order.getAmount());
                }
            } catch (Exception e) {
                log.error("增加商家余额失败：orderNo={}", orderNo, e);
                // 商家余额增加失败不影响订单支付流程
            }
        }
        
        log.info("订单支付成功：{}", orderNo);
    }
    
    /**
     * 生成订单号
     */
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
        
        // 订单状态过滤
        if (orderStatus != null && !orderStatus.isEmpty()) {
            wrapper.eq(PaymentOrder::getOrderStatus, orderStatus);
        }
        
        return page(page, wrapper);
    }
}

