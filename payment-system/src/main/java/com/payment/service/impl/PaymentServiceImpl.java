package com.payment.service.impl;

import com.payment.util.JsonUtils;
import com.alipay.api.AlipayApiException;
import com.alipay.api.AlipayClient;
import com.alipay.api.DefaultAlipayClient;
import com.alipay.api.domain.AlipayTradePagePayModel;
import com.alipay.api.request.AlipayTradePagePayRequest;
import com.alipay.api.response.AlipayTradePagePayResponse;
import com.payment.config.PaymentConfig;
import com.payment.dto.PayResponseDTO;
import com.payment.entity.PaymentOrder;
import com.payment.entity.User;
import com.payment.service.PaymentService;
import com.payment.service.UserService;
import com.wechat.pay.java.core.Config;
import com.wechat.pay.java.core.RSAAutoCertificateConfig;
import com.wechat.pay.java.core.notification.NotificationParser;
import com.wechat.pay.java.core.notification.RequestParam;
import com.wechat.pay.java.service.payments.jsapi.JsapiServiceExtension;
import com.wechat.pay.java.service.payments.jsapi.model.Payer;
import com.wechat.pay.java.service.payments.jsapi.model.PrepayWithRequestPaymentResponse;
import com.wechat.pay.java.service.payments.nativepay.NativePayService;
import com.wechat.pay.java.service.payments.nativepay.model.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

/**
 * 支付服务实现类
 */
@Slf4j
@Service
public class PaymentServiceImpl implements PaymentService {
    
    @Autowired
    private PaymentConfig paymentConfig;

    @Autowired
    private UserService userService;
    
    private Config wechatConfig;
    private NativePayService nativePayService;
    private JsapiServiceExtension jsapiService;
    private NotificationParser notificationParser;
    
    /**
     * 初始化微信支付配置
     */
    private void initWechatConfig() {
        if (wechatConfig == null) {
            try {
                wechatConfig = new RSAAutoCertificateConfig.Builder()
                        .merchantId(paymentConfig.getWechat().getMchId())
                        .privateKeyFromPath(paymentConfig.getWechat().getKeyPath())
                        .merchantSerialNumber(getMerchantSerialNumber())
                        .apiV3Key(paymentConfig.getWechat().getApiV3Key())
                        .build();
                nativePayService = new NativePayService.Builder().config(wechatConfig).build();
                jsapiService = new JsapiServiceExtension.Builder().config(wechatConfig).build();
                notificationParser = new NotificationParser((com.wechat.pay.java.core.notification.NotificationConfig) wechatConfig);
            } catch (Exception e) {
                log.error("初始化微信支付配置失败", e);
                throw new RuntimeException("初始化微信支付配置失败：" + e.getMessage());
            }
        }
    }
    
    /**
     * 获取商户证书序列号（实际应该从证书文件中读取）
     */
    private String getMerchantSerialNumber() {
        // 实际应该从证书文件中读取序列号，或者在RSAAutoCertificateConfig中自动获取
        // 如果使用了RSAAutoCertificateConfig，通常不需要手动提供序列号，因为它会从证书中读取
        return paymentConfig.getWechat().getMerchantSerialNumber();
    }
    
    @Override
    public PayResponseDTO createPay(PaymentOrder order) {
        String payType = order.getPayType();
        
        if ("WECHAT".equals(payType)) {
            if ("JSAPI".equals(order.getTradeType())) {
                return createJsapiPay(order);
            } else {
                return createWechatPay(order);
            }
        } else if ("ALIPAY".equals(payType)) {
            return createAlipayPay(order);
        } else {
            throw new RuntimeException("不支持的支付方式：" + payType);
        }
    }

    /**
     * 创建微信JSAPI支付（小程序/公众号）
     */
    private PayResponseDTO createJsapiPay(PaymentOrder order) {
        try {
            initWechatConfig();

            // 获取用户OpenID
            User user = userService.getById(order.getUserId());
            if (user == null) {
                throw new RuntimeException("用户不存在");
            }
            // 假设username存储了openid
            String openid = user.getUsername();
            if (!openid.startsWith("wx_")) {
                 // 尝试从其他地方获取，或者如果username不是openid，这里需要调整
                 // 这里的逻辑基于 UserServiceImpl.wechatLogin 的实现
                 log.warn("用户 {} 的用户名 {} 不像是一个OpenID", user.getId(), user.getUsername());
            }

            // 构建JSAPI支付请求
            com.wechat.pay.java.service.payments.jsapi.model.PrepayRequest request = new com.wechat.pay.java.service.payments.jsapi.model.PrepayRequest();
            com.wechat.pay.java.service.payments.jsapi.model.Amount amount = new com.wechat.pay.java.service.payments.jsapi.model.Amount();
            amount.setTotal(order.getAmount().multiply(new java.math.BigDecimal("100")).intValue());
            request.setAmount(amount);
            request.setAppid(paymentConfig.getWechat().getAppId());
            request.setMchid(paymentConfig.getWechat().getMchId());
            request.setDescription(order.getSubject());
            request.setNotifyUrl(paymentConfig.getWechat().getNotifyUrl());
            request.setOutTradeNo(order.getOrderNo());
            
            Payer payer = new Payer();
            payer.setOpenid(openid);
            request.setPayer(payer);

            // 调用微信支付API，直接获取带有签名的支付参数
            PrepayWithRequestPaymentResponse response = jsapiService.prepayWithRequestPayment(request);

            PayResponseDTO payResponse = new PayResponseDTO();
            payResponse.setOrderNo(order.getOrderNo());
            payResponse.setPayType("WECHAT_JSAPI");
            payResponse.setAmount(order.getAmount());
            // 将支付参数序列化为JSON字符串放入payUrl字段，或者新增字段存储
            // 这里为了兼容性，我们将参数拼接成JSON字符串
            Map<String, String> payParams = new HashMap<>();
            payParams.put("timeStamp", response.getTimeStamp());
            payParams.put("nonceStr", response.getNonceStr());
            payParams.put("package", response.getPackageVal());
            payParams.put("signType", response.getSignType());
            payParams.put("paySign", response.getPaySign());
            
            payResponse.setPayUrl(JsonUtils.toJson(payParams));

            log.info("创建微信JSAPI支付订单成功：{}", order.getOrderNo());
            return payResponse;
        } catch (Exception e) {
            log.error("创建微信JSAPI支付失败", e);
            throw new RuntimeException("创建微信JSAPI支付失败：" + e.getMessage());
        }
    }
    
    /**
     * 创建微信Native支付
     */
    private PayResponseDTO createWechatPay(PaymentOrder order) {
        try {
            initWechatConfig();
            
            // 构建微信Native支付请求
            PrepayRequest request = new PrepayRequest();
            Amount amount = new Amount();
            amount.setTotal(order.getAmount().multiply(new java.math.BigDecimal("100")).intValue()); // 转换为分
            request.setAmount(amount);
            request.setAppid(paymentConfig.getWechat().getAppId());
            request.setMchid(paymentConfig.getWechat().getMchId());
            request.setDescription(order.getSubject());
            request.setNotifyUrl(paymentConfig.getWechat().getNotifyUrl());
            request.setOutTradeNo(order.getOrderNo());
            
            // 调用微信支付API
            PrepayResponse response = nativePayService.prepay(request);
            
            PayResponseDTO payResponse = new PayResponseDTO();
            payResponse.setOrderNo(order.getOrderNo());
            payResponse.setPayType("WECHAT");
            payResponse.setAmount(order.getAmount());
            payResponse.setQrCode(response.getCodeUrl()); // 微信支付二维码URL
            payResponse.setPayUrl("https://api.mch.weixin.qq.com/v3/pay/transactions/native");
            
            log.info("创建微信Native支付订单成功：{}, 二维码：{}", order.getOrderNo(), response.getCodeUrl());
            return payResponse;
        } catch (Exception e) {
            log.error("创建微信Native支付失败", e);
            throw new RuntimeException("创建微信Native支付失败：" + e.getMessage());
        }
    }
    
    /**
     * 创建支付宝支付
     */
    private PayResponseDTO createAlipayPay(PaymentOrder order) {
        try {
            AlipayClient alipayClient = new DefaultAlipayClient(
                    paymentConfig.getAlipay().getGatewayUrl(),
                    paymentConfig.getAlipay().getAppId(),
                    paymentConfig.getAlipay().getPrivateKey(),
                    "json",
                    "UTF-8",
                    paymentConfig.getAlipay().getPublicKey(),
                    "RSA2"
            );
            
            AlipayTradePagePayModel model = new AlipayTradePagePayModel();
            model.setOutTradeNo(order.getOrderNo());
            model.setTotalAmount(order.getAmount().toString());
            model.setSubject(order.getSubject());
            model.setBody(order.getBody());
            model.setProductCode("FAST_INSTANT_TRADE_PAY");
            
            AlipayTradePagePayRequest request = new AlipayTradePagePayRequest();
            request.setBizModel(model);
            request.setNotifyUrl(paymentConfig.getAlipay().getNotifyUrl());
            request.setReturnUrl(paymentConfig.getAlipay().getReturnUrl());
            
            AlipayTradePagePayResponse response = alipayClient.pageExecute(request);
            
            PayResponseDTO payResponse = new PayResponseDTO();
            payResponse.setOrderNo(order.getOrderNo());
            payResponse.setPayType("ALIPAY");
            payResponse.setAmount(order.getAmount());
            payResponse.setPayUrl(response.getBody());
            
            log.info("创建支付宝支付订单：{}", order.getOrderNo());
            return payResponse;
        } catch (AlipayApiException e) {
            log.error("创建支付宝支付失败", e);
            throw new RuntimeException("创建支付宝支付失败：" + e.getMessage());
        }
    }
    
    @Override
    public boolean verifyNotify(String payType, Map<String, String> params) {
        if ("WECHAT".equals(payType)) {
            return verifyWechatNotify(params);
        } else if ("ALIPAY".equals(payType)) {
            return verifyAlipayNotify(params);
        }
        return false;
    }
    
    /**
     * 验证微信支付回调
     */
    private boolean verifyWechatNotify(Map<String, String> params) {
        try {
            initWechatConfig();
            
            // 构建请求参数
            RequestParam requestParam = new RequestParam.Builder()
                    .serialNumber(params.get("Wechatpay-Serial"))
                    .nonce(params.get("Wechatpay-Nonce"))
                    .signature(params.get("Wechatpay-Signature"))
                    .timestamp(params.get("Wechatpay-Timestamp"))
                    .body(params.get("body"))
                    .build();
            
            // 验证签名并解析通知
            notificationParser.parse(requestParam, com.wechat.pay.java.service.payments.model.Transaction.class);
            
            log.info("微信支付回调签名验证成功");
            return true;
        } catch (Exception e) {
            log.error("微信支付回调签名验证失败", e);
            return false;
        }
    }
    
    /**
     * 验证支付宝支付回调
     */
    private boolean verifyAlipayNotify(Map<String, String> params) {
        // 实际应该验证支付宝签名
        // 这里只是示例
        return true;
    }
    
    @Override
    public Map<String, String> queryOrder(String payType, String orderNo) {
        Map<String, String> result = new HashMap<>();
        
        if ("WECHAT".equals(payType)) {
            result = queryWechatOrder(orderNo);
        } else if ("ALIPAY".equals(payType)) {
            result = queryAlipayOrder(orderNo);
        } else {
            result.put("status", "UNKNOWN");
        }
        
        return result;
    }
    
    /**
     * 查询微信支付订单
     */
    private Map<String, String> queryWechatOrder(String orderNo) {
        Map<String, String> result = new HashMap<>();
        try {
            initWechatConfig();
            
            // 构建查询请求
            QueryOrderByOutTradeNoRequest request = new QueryOrderByOutTradeNoRequest();
            request.setMchid(paymentConfig.getWechat().getMchId());
            request.setOutTradeNo(orderNo);
            
            // 调用微信支付查询API
            com.wechat.pay.java.service.payments.model.Transaction transaction = 
                    nativePayService.queryOrderByOutTradeNo(request);
            
            // 映射支付状态
            String status = mapWechatTradeState(transaction.getTradeState());
            result.put("status", status);
            result.put("tradeNo", transaction.getTransactionId());
            result.put("tradeState", transaction.getTradeState().name());
            
            log.info("查询微信支付订单成功：{}, 状态：{}", orderNo, status);
        } catch (Exception e) {
            log.error("查询微信支付订单失败：{}", orderNo, e);
            result.put("status", "QUERY_FAILED");
            result.put("error", e.getMessage());
        }
        return result;
    }
    
    /**
     * 映射微信支付状态到系统状态
     */
    private String mapWechatTradeState(com.wechat.pay.java.service.payments.model.Transaction.TradeStateEnum tradeState) {
        switch (tradeState) {
            case SUCCESS:
                return "SUCCESS";
            case REFUND:
                return "REFUND";
            case NOTPAY:
                return "NOTPAY";
            case CLOSED:
                return "CLOSED";
            case REVOKED:
                return "REVOKED";
            case USERPAYING:
                return "USERPAYING";
            case PAYERROR:
                return "PAYERROR";
            default:
                return "UNKNOWN";
        }
    }
    
    /**
     * 查询支付宝订单
     */
    private Map<String, String> queryAlipayOrder(String orderNo) {
        Map<String, String> result = new HashMap<>();
        // TODO: 实现支付宝订单查询
        result.put("status", "SUCCESS");
        return result;
    }
}

