package com.payment.controller;

import com.payment.dto.PaymentCallbackDTO;
import com.payment.service.PaymentOrderService;
import com.payment.service.PaymentSignatureVerifier;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 支付回调控制器（遗留路径）。
 * @deprecated 请使用 V1OpenPaymentController 的 /v1/open/payments/ 路径
 * <p>
 * 已废弃：V1OpenPaymentController 提供了更完善的回调处理路径。
 * <p>
 * 可通过配置 {@code legacy.payment.controller.enabled=true} 启用，否则生产环境不加载此 Bean。
 * 默认禁用（matchIfMissing = false），防止遗留路径绕过 V1 验签逻辑。
 */
@Slf4j
@RestController
@RequestMapping("/payment")
@Deprecated(since = "v1", forRemoval = true)
@ConditionalOnProperty(name = "legacy.payment.controller.enabled", havingValue = "true", matchIfMissing = false)
@Profile({"dev", "test"})
public class PaymentController {

    @Autowired
    private PaymentOrderService paymentOrderService;

    @Autowired
    private PaymentSignatureVerifier signatureVerifier;

    @PostMapping("/wechat/notify")
    public String wechatNotify(HttpServletRequest request) {
        try {
            Map<String, String> params = request.getParameterMap().entrySet().stream()
                    .collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue()[0]));

            PaymentCallbackDTO dto = new PaymentCallbackDTO();
            dto.setBillNo(params.get("orderNo"));
            dto.setRawBody(params.get("body"));
            if (!signatureVerifier.verify("WECHAT", dto, params)) {
                log.warn("[SECURITY] 微信回调验签失败，拒绝处理, orderNo={}", params.get("orderNo"));
                return "FAIL";
            }

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

            PaymentCallbackDTO dto = new PaymentCallbackDTO();
            dto.setBillNo(params.get("out_trade_no"));
            dto.setThirdPartyBillNo(params.get("trade_no"));
            dto.setSuccess("TRADE_SUCCESS".equals(params.get("trade_status"))
                    || "TRADE_FINISHED".equals(params.get("trade_status")));
            dto.setRawBody(params.toString());
            if (!signatureVerifier.verify("ALIPAY", dto, params)) {
                log.warn("[SECURITY] 支付宝回调验签失败，拒绝处理, out_trade_no={}", params.get("out_trade_no"));
                return "fail";
            }

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
