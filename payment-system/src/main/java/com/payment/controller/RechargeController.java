package com.payment.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.payment.annotation.RequireAuth;
import com.payment.common.Result;
import com.payment.dto.CreateRechargeOrderDTO;
import com.payment.dto.PayResponseDTO;
import com.payment.dto.RechargeRuleDTO;
import com.payment.entity.BalanceLog;
import com.payment.entity.PaymentOrder;
import com.payment.entity.RechargeOrder;
import com.payment.entity.RechargeRule;
import com.payment.service.PaymentOrderService;
import com.payment.service.PaymentService;
import com.payment.service.RechargeService;
import com.payment.util.TenantContextHolder;
import com.payment.util.UserContextHolder;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 充值管理控制器
 */
@Slf4j
@RestController
@RequestMapping("/api/recharge")
 
 
public class RechargeController {
    
    @Autowired
    private RechargeService rechargeService;
    
    @Autowired
    private PaymentOrderService paymentOrderService;
    
    @Autowired
    private PaymentService paymentService;
    
    /**
     * 获取商家充值规则（商家端）
     */
    @RequireAuth
    @GetMapping("/rules")

    public Result<List<RechargeRule>> getRechargeRules() {
        Long tenantId = TenantContextHolder.getTenantId();
        List<RechargeRule> rules = rechargeService.getRechargeRules(tenantId);
        return Result.success(rules);
    }
    
    /**
     * 设置商家充值规则（商家端）
     */
    @RequireAuth
    @PostMapping("/rules")
    public Result<Void> setRechargeRules(@Validated @RequestBody List<RechargeRuleDTO> rules) {
        rechargeService.setRechargeRules(rules);
        return Result.success();
    }
    
    /**
     * 获取用户端充值规则列表（用户端）
     */
    @RequireAuth
    @GetMapping("/user/rules")
    public Result<List<RechargeRule>> getUserRechargeRules() {
        Long tenantId = TenantContextHolder.getTenantId();
        List<RechargeRule> rules = rechargeService.getRechargeRules(tenantId);
        return Result.success(rules);
    }
    
    /**
     * 创建充值订单（用户端）
     */
    @RequireAuth
    @PostMapping("/order")
    public Result<Map<String, Object>> createRechargeOrder(@Validated @RequestBody CreateRechargeOrderDTO dto) {
        Long userId = UserContextHolder.getUserId();
        
        // 创建充值订单
        RechargeOrder rechargeOrder = rechargeService.createRechargeOrder(userId, dto.getRuleId());
        
        // 创建支付订单
        PaymentOrder paymentOrder = new PaymentOrder();
        paymentOrder.setOrderNo(rechargeOrder.getOrderNo());
        paymentOrder.setTenantId(rechargeOrder.getTenantId());
        paymentOrder.setUserId(userId);
        paymentOrder.setAmount(rechargeOrder.getRechargeAmount());
        paymentOrder.setPayType("WECHAT");
        paymentOrder.setSubject("账户充值");
        paymentOrder.setBody("充值金额：" + rechargeOrder.getRechargeAmount() + "元，赠送：" + rechargeOrder.getBonusAmount() + "元");
        
        // 调用支付服务
        PayResponseDTO payResponse = paymentService.createPay(paymentOrder);
        
        Map<String, Object> result = new HashMap<>();
        result.put("orderNo", rechargeOrder.getOrderNo());
        result.put("rechargeAmount", rechargeOrder.getRechargeAmount());
        result.put("bonusAmount", rechargeOrder.getBonusAmount());
        result.put("totalAmount", rechargeOrder.getTotalAmount());
        result.put("payInfo", payResponse);
        
        return Result.success(result);
    }
    
    /**
     * 查询用户余额（用户端）
     */
    @RequireAuth
    @GetMapping("/balance")
    public Result<Map<String, Object>> getUserBalance() {
        Long userId = UserContextHolder.getUserId();
        Long tenantId = TenantContextHolder.getTenantId();
        
        BigDecimal balance = rechargeService.getUserBalance(userId, tenantId);
        
        Map<String, Object> result = new HashMap<>();
        result.put("balance", balance);
        result.put("userId", userId);
        result.put("tenantId", tenantId);
        
        return Result.success(result);
    }
    
    /**
     * 查询余额明细（用户端）
     */
    @RequireAuth
    @GetMapping("/balance/logs")
    public Result<Page<BalanceLog>> listBalanceLogs(
               @RequestParam(defaultValue = "1") Integer pageNum,
               @RequestParam(defaultValue = "10") Integer pageSize) {
        Long userId = UserContextHolder.getUserId();
        Long tenantId = TenantContextHolder.getTenantId();
        
        Page<BalanceLog> page = rechargeService.listBalanceLogs(userId, tenantId, pageNum, pageSize);
        return Result.success(page);
    }
}
