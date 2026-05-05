package com.payment.service.impl;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.payment.common.BusinessException;
import com.payment.config.PaymentConfig;
import com.payment.dto.ExternalPaymentQueryResult;
import com.payment.dto.PayResponseDTO;
import com.payment.dto.PaymentCallbackDTO;
import com.payment.dto.RefundQueryResultDTO;
import com.payment.dto.RefundRequestDTO;
import com.payment.dto.RefundSubmitResultDTO;
import com.payment.entity.PaymentBill;
import com.payment.entity.RefundRecord;
import com.payment.enums.PaymentChannelCodeEnum;
import com.payment.enums.RefundChannelStatusEnum;
import com.payment.service.PaymentProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class ExtPaymentProviderImpl implements PaymentProvider {

    private static final String SIGN_TYPE = "MD5";

    private final PaymentConfig paymentConfig;
    private final RestTemplate restTemplate;

    @Override
    public String getChannelCode() {
        return PaymentChannelCodeEnum.EXT_PROVIDER.name();
    }

    @Override
    public PayResponseDTO createPayment(PaymentBill paymentBill) {
        PaymentConfig.ExtProvider config = requireConfig();

        Map<String, String> sortedParams = new LinkedHashMap<>();
        sortedParams.put("pid", config.getMerchantId());
        sortedParams.put("type", defaultIfBlank(config.getDefaultPayType(), "alipay"));
        sortedParams.put("out_trade_no", paymentBill.getBillNo());
        sortedParams.put("notify_url", config.getNotifyUrl());
        if (StringUtils.hasText(config.getReturnUrl())) {
            sortedParams.put("return_url", config.getReturnUrl());
        }
        sortedParams.put("name", buildSubject(paymentBill));
        sortedParams.put("money", formatAmount(paymentBill.getPayAmount()));
        sortedParams.put("clientip", defaultIfBlank(config.getClientIp(), "127.0.0.1"));
        sortedParams.put("device", defaultIfBlank(config.getDefaultDevice(), "pc"));
        sortedParams.put("param", paymentBill.getBizNo());

        String sign = sign(sortedParams, config.getMerchantKey());

        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        sortedParams.forEach(form::add);
        form.add("sign", sign);
        form.add("sign_type", SIGN_TYPE);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        String responseBody = restTemplate.postForObject(
                config.getBaseUrl() + "/mapi.php",
                new HttpEntity<>(form, headers),
                String.class
        );
        JSONObject response = parseResponse(responseBody);
        if (!isSuccessCode(response.getString("code"))) {
            throw new BusinessException(defaultIfBlank(response.getString("msg"), "External provider payment creation failed"));
        }

        PayResponseDTO responseDTO = new PayResponseDTO();
        responseDTO.setOrderNo(paymentBill.getBillNo());
        responseDTO.setPayType(paymentBill.getChannelCode());
        responseDTO.setAmount(paymentBill.getPayAmount());
        responseDTO.setPayUrl(firstNonBlank(
                response.getString("payurl"),
                response.getString("qrcode"),
                response.getString("urlscheme")
        ));
        responseDTO.setQrCode(response.getString("qrcode"));
        return responseDTO;
    }

    @Override
    public boolean verifyCallback(PaymentCallbackDTO callbackDTO) {
        if (callbackDTO.getSuccess() == null || !callbackDTO.getSuccess() || callbackDTO.getBillNo() == null) {
            return false;
        }
        if (!StringUtils.hasText(callbackDTO.getRawBody())) {
            return false;
        }

        PaymentConfig.ExtProvider config = requireConfig();
        JSONObject body = parseResponse(callbackDTO.getRawBody());
        String sign = body.getString("sign");
        String signType = body.getString("sign_type");
        if (!StringUtils.hasText(sign) || !SIGN_TYPE.equalsIgnoreCase(signType)) {
            return false;
        }
        if (!"TRADE_SUCCESS".equals(body.getString("trade_status"))) {
            return false;
        }

        Map<String, String> params = new LinkedHashMap<>();
        for (String key : body.keySet()) {
            if ("sign".equals(key) || "sign_type".equals(key)) {
                continue;
            }
            String value = body.getString(key);
            if (StringUtils.hasText(value)) {
                params.put(key, value);
            }
        }
        String expectedSign = sign(params, config.getMerchantKey());
        return sign.equalsIgnoreCase(expectedSign);
    }

    @Override
    public ExternalPaymentQueryResult queryPayment(PaymentBill paymentBill) {
        PaymentConfig.ExtProvider config = requireConfig();

        UriComponentsBuilder builder = UriComponentsBuilder
                .fromHttpUrl(config.getBaseUrl() + "/api.php")
                .queryParam("act", "order")
                .queryParam("pid", config.getMerchantId())
                .queryParam("key", config.getMerchantKey());

        if (StringUtils.hasText(paymentBill.getThirdPartyBillNo())) {
            builder.queryParam("trade_no", paymentBill.getThirdPartyBillNo());
        } else {
            builder.queryParam("out_trade_no", paymentBill.getBillNo());
        }

        String responseBody = restTemplate.getForObject(builder.build(true).toUri(), String.class);
        JSONObject response = parseResponse(responseBody);

        ExternalPaymentQueryResult result = new ExternalPaymentQueryResult();
        result.setSuccess(isSuccessCode(response.getString("code")));
        result.setMessage(response.getString("msg"));
        result.setProviderTradeNo(response.getString("trade_no"));
        result.setChannelTradeNo(response.getString("api_trade_no"));
        result.setRawStatus(response.getString("status"));
        result.setBuyer(response.getString("buyer"));
        result.setPaid("1".equals(response.getString("status")));
        return result;
    }

    @Override
    public boolean supportsRefund() {
        return false;
    }

    @Override
    public RefundSubmitResultDTO refund(PaymentBill paymentBill, RefundRequestDTO requestDTO) {
        RefundSubmitResultDTO result = new RefundSubmitResultDTO();
        result.setSuccess(false);
        result.setChannelStatus(RefundChannelStatusEnum.FAIL.name());
        result.setRawStatus("UNSUPPORTED");
        result.setMessage("EXT_PROVIDER refund is not supported in phase 1");
        return result;
    }

    @Override
    public RefundQueryResultDTO queryRefund(RefundRecord refundRecord) {
        RefundQueryResultDTO result = new RefundQueryResultDTO();
        result.setSuccess(false);
        result.setChannelStatus(RefundChannelStatusEnum.FAIL.name());
        result.setRawStatus("UNSUPPORTED");
        result.setMessage("EXT_PROVIDER refund query is not supported in phase 1");
        return result;
    }

    private PaymentConfig.ExtProvider requireConfig() {
        PaymentConfig.ExtProvider config = paymentConfig.getExtProvider();
        if (config == null || !StringUtils.hasText(config.getBaseUrl())
                || !StringUtils.hasText(config.getMerchantId())
                || !StringUtils.hasText(config.getMerchantKey())
                || !StringUtils.hasText(config.getNotifyUrl())) {
            throw new BusinessException("External provider payment config is incomplete");
        }
        return config;
    }

    private JSONObject parseResponse(String responseBody) {
        if (!StringUtils.hasText(responseBody)) {
            throw new BusinessException("External provider returned an empty response");
        }
        try {
            return JSON.parseObject(responseBody);
        } catch (Exception e) {
            log.error("Failed to parse external provider response: {}", responseBody, e);
            throw new BusinessException("External provider returned an invalid response");
        }
    }

    private String buildSubject(PaymentBill paymentBill) {
        return "PAY-" + paymentBill.getBizType() + "-" + paymentBill.getBillNo();
    }

    private String formatAmount(BigDecimal amount) {
        return amount.setScale(2, RoundingMode.HALF_UP).toPlainString();
    }

    private String sign(Map<String, String> params, String merchantKey) {
        String raw = params.entrySet().stream()
                .filter(entry -> StringUtils.hasText(entry.getValue()))
                .sorted(Map.Entry.comparingByKey(Comparator.naturalOrder()))
                .map(entry -> entry.getKey() + "=" + entry.getValue())
                .collect(Collectors.joining("&"));
        return md5(raw + merchantKey);
    }

    private String md5(String content) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] digest = md.digest(content.getBytes(StandardCharsets.UTF_8));
            StringBuilder builder = new StringBuilder();
            for (byte b : digest) {
                builder.append(String.format("%02x", b));
            }
            return builder.toString();
        } catch (Exception e) {
            throw new IllegalStateException("MD5 sign failed", e);
        }
    }

    private boolean isSuccessCode(String code) {
        return "1".equals(code);
    }

    private String firstNonBlank(String... values) {
        for (String value : values) {
            if (StringUtils.hasText(value)) {
                return value;
            }
        }
        return null;
    }

    private String defaultIfBlank(String value, String fallback) {
        return StringUtils.hasText(value) ? value : fallback;
    }
}
