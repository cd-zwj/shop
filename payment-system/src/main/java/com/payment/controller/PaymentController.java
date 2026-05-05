package com.payment.controller;

import com.payment.service.PaymentOrderService;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 支付回调控制器
 */
@Slf4j
 
 
@RestController
@RequestMapping("/payment")
public class PaymentController {
    
    @Autowired
    private PaymentOrderService paymentOrderService;

    @PostMapping("/wechat/notify")
    public String wechatNotify(HttpServletRequest request) {
        try {
            Map<String, String> params = request.getParameterMap().entrySet().stream()
                    .collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue()[0]));
            paymentOrderService.handlePayNotify("WECHAT", params);
            return "SUCCESS";
        } catch (Exception e) {
            log.error("微信支付回调处理失败", e);
            return "FAIL";
        }
    }
    

    @PostMapping("/alipay/notify")
    public String alipayNotify(HttpServletRequest request) {
        try {
            Map<String, String> params = request.getParameterMap().entrySet().stream()
                    .collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue()[0]));
            
            paymentOrderService.handlePayNotify("ALIPAY", params);
            return "success";
        } catch (Exception e) {
            log.error("支付宝支付回调处理失败", e);
            return "fail";
        }
    }

    @GetMapping("/alipay/return")
    public String alipayReturn(HttpServletRequest request) {
        // 支付宝同步回调，通常用于页面跳转
        return "支付成功";
    }
}

