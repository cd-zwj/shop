package com.payment.service.impl;

import com.payment.util.JsonUtils;
import com.fasterxml.jackson.core.type.TypeReference;
import com.alipay.api.AlipayApiException;
import com.alipay.api.AlipayClient;
import com.alipay.api.DefaultAlipayClient;
import com.alipay.api.domain.AlipayTradeFastpayRefundQueryModel;
import com.alipay.api.domain.AlipayTradePagePayModel;
import com.alipay.api.domain.AlipayTradeQueryModel;
import com.alipay.api.domain.AlipayTradeRefundModel;
import com.alipay.api.internal.util.AlipaySignature;
import com.alipay.api.request.AlipayTradeFastpayRefundQueryRequest;
import com.alipay.api.request.AlipayTradePagePayRequest;
import com.alipay.api.request.AlipayTradeQueryRequest;
import com.alipay.api.request.AlipayTradeRefundRequest;
import com.alipay.api.response.AlipayTradeFastpayRefundQueryResponse;
import com.alipay.api.response.AlipayTradePagePayResponse;
import com.alipay.api.response.AlipayTradeQueryResponse;
import com.alipay.api.response.AlipayTradeRefundResponse;
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
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.math.RoundingMode;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class AlipayPagePaymentProvider implements PaymentProvider {

    private static final String FORMAT = "json";
    private static final String CHARSET = "UTF-8";
    private static final String SIGN_TYPE = "RSA2";
    private static final String PRODUCT_CODE = "FAST_INSTANT_TRADE_PAY";
    private static final String REFUND_SUCCESS_STATUS = "REFUND_SUCCESS";
    private static final String REFUND_FAIL_STATUS = "REFUND_FAIL";

    private final PaymentConfig paymentConfig;
    private volatile AlipayClient alipayClient;

    @Override
    public String getChannelCode() {
        return PaymentChannelCodeEnum.ALIPAY_PAGE.name();
    }

    @Override
    public PayResponseDTO createPayment(PaymentBill paymentBill) {
        try {
            PaymentConfig.Alipay config = requireConfig();
            AlipayTradePagePayModel model = new AlipayTradePagePayModel();
            model.setOutTradeNo(paymentBill.getBillNo());
            model.setTotalAmount(paymentBill.getPayAmount().setScale(2, RoundingMode.HALF_UP).toPlainString());
            model.setSubject(buildSubject(paymentBill));
            model.setBody(buildBody(paymentBill));
            model.setProductCode(PRODUCT_CODE);
            model.setTimeoutExpress("30m");

            AlipayTradePagePayRequest request = new AlipayTradePagePayRequest();
            request.setBizModel(model);
            request.setNotifyUrl(config.getNotifyUrl());
            request.setReturnUrl(config.getReturnUrl());

            AlipayTradePagePayResponse response = getClient(config).pageExecute(request);
            if (!response.isSuccess() || !StringUtils.hasText(response.getBody())) {
                throw new BusinessException("Alipay payment creation failed: " + firstNonBlank(response.getSubMsg(), response.getMsg()));
            }

            PayResponseDTO payResponseDTO = new PayResponseDTO();
            payResponseDTO.setOrderNo(paymentBill.getBillNo());
            payResponseDTO.setPayType(getChannelCode());
            payResponseDTO.setAmount(paymentBill.getPayAmount());
            payResponseDTO.setPayUrl(response.getBody());
            return payResponseDTO;
        } catch (AlipayApiException e) {
            log.error("Failed to create alipay page payment, billNo={}", paymentBill.getBillNo(), e);
            throw new BusinessException("Alipay payment creation failed: " + e.getErrMsg());
        }
    }

    @Override
    public boolean verifyCallback(PaymentCallbackDTO callbackDTO) {
        if (!StringUtils.hasText(callbackDTO.getRawBody())) {
            return false;
        }

        try {
            PaymentConfig.Alipay config = requireConfig();
            Map<String, String> params = JsonUtils.fromJson(
                    callbackDTO.getRawBody(),
                    new TypeReference<Map<String, String>>() {
                    }
            );
            if (params == null || params.isEmpty()) {
                return false;
            }

            boolean signatureValid = AlipaySignature.rsaCheckV1(
                    params,
                    config.getPublicKey(),
                    CHARSET,
                    SIGN_TYPE
            );
            if (!signatureValid) {
                return false;
            }

            String tradeStatus = params.get("trade_status");
            return "TRADE_SUCCESS".equals(tradeStatus) || "TRADE_FINISHED".equals(tradeStatus);
        } catch (Exception e) {
            log.error("Failed to verify alipay callback, billNo={}", callbackDTO.getBillNo(), e);
            return false;
        }
    }

    @Override
    public ExternalPaymentQueryResult queryPayment(PaymentBill paymentBill) {
        try {
            PaymentConfig.Alipay config = requireConfig();
            AlipayTradeQueryModel model = new AlipayTradeQueryModel();
            model.setOutTradeNo(paymentBill.getBillNo());
            if (StringUtils.hasText(paymentBill.getThirdPartyBillNo())) {
                model.setTradeNo(paymentBill.getThirdPartyBillNo());
            }

            AlipayTradeQueryRequest request = new AlipayTradeQueryRequest();
            request.setBizModel(model);

            AlipayTradeQueryResponse response = getClient(config).execute(request);

            ExternalPaymentQueryResult result = new ExternalPaymentQueryResult();
            result.setSuccess(response.isSuccess());
            result.setMessage(firstNonBlank(response.getSubMsg(), response.getMsg()));
            result.setProviderTradeNo(response.getTradeNo());
            result.setChannelTradeNo(response.getTradeNo());
            result.setRawStatus(response.getTradeStatus());
            result.setBuyer(response.getBuyerLogonId());
            result.setPaid("TRADE_SUCCESS".equals(response.getTradeStatus())
                    || "TRADE_FINISHED".equals(response.getTradeStatus()));
            return result;
        } catch (AlipayApiException e) {
            log.error("Failed to query alipay payment, billNo={}", paymentBill.getBillNo(), e);
            throw new BusinessException("Alipay payment query failed: " + e.getErrMsg());
        }
    }

    @Override
    public boolean supportsRefund() {
        return true;
    }

    @Override
    public RefundSubmitResultDTO refund(PaymentBill paymentBill, RefundRequestDTO requestDTO) {
        try {
            PaymentConfig.Alipay config = requireConfig();
            AlipayTradeRefundModel model = new AlipayTradeRefundModel();
            model.setOutTradeNo(paymentBill.getBillNo());
            if (StringUtils.hasText(paymentBill.getThirdPartyBillNo())) {
                model.setTradeNo(paymentBill.getThirdPartyBillNo());
            }
            model.setRefundAmount(requestDTO.getRefundAmount().setScale(2, RoundingMode.HALF_UP).toPlainString());
            model.setRefundReason(requestDTO.getRefundReason());
            model.setOutRequestNo(requestDTO.getRefundNo());

            AlipayTradeRefundRequest request = new AlipayTradeRefundRequest();
            request.setBizModel(model);

            AlipayTradeRefundResponse response = getClient(config).execute(request);

            RefundSubmitResultDTO result = new RefundSubmitResultDTO();
            result.setSuccess(response.isSuccess());
            result.setProviderRefundNo(requestDTO.getRefundNo());
            result.setRawStatus(response.getCode());
            result.setMessage(firstNonBlank(response.getSubMsg(), response.getMsg()));
            result.setChannelStatus(response.isSuccess()
                    ? RefundChannelStatusEnum.SUCCESS.name()
                    : RefundChannelStatusEnum.FAIL.name());
            return result;
        } catch (AlipayApiException e) {
            log.error("Failed to submit alipay refund, billNo={}, refundNo={}", paymentBill.getBillNo(), requestDTO.getRefundNo(), e);
            throw new BusinessException("Alipay refund failed: " + e.getErrMsg());
        }
    }

    @Override
    public RefundQueryResultDTO queryRefund(RefundRecord refundRecord) {
        try {
            PaymentConfig.Alipay config = requireConfig();
            AlipayTradeFastpayRefundQueryModel model = new AlipayTradeFastpayRefundQueryModel();
            model.setOutRequestNo(refundRecord.getRefundNo());
            if (StringUtils.hasText(refundRecord.getPaymentBillNo())) {
                model.setOutTradeNo(refundRecord.getPaymentBillNo());
            }
            if (StringUtils.hasText(refundRecord.getThirdPartyBillNo())) {
                model.setTradeNo(refundRecord.getThirdPartyBillNo());
            }

            AlipayTradeFastpayRefundQueryRequest request = new AlipayTradeFastpayRefundQueryRequest();
            request.setBizModel(model);

            AlipayTradeFastpayRefundQueryResponse response = getClient(config).execute(request);

            RefundQueryResultDTO result = new RefundQueryResultDTO();
            result.setSuccess(response.isSuccess());
            result.setProviderRefundNo(firstNonBlank(response.getOutRequestNo(), refundRecord.getRefundNo()));
            result.setRawStatus(response.getRefundStatus());
            result.setMessage(firstNonBlank(response.getSubMsg(), response.getMsg()));
            result.setChannelStatus(mapRefundQueryStatus(response.isSuccess(), response.getRefundStatus()));
            return result;
        } catch (AlipayApiException e) {
            log.error("Failed to query alipay refund, refundNo={}", refundRecord.getRefundNo(), e);
            throw new BusinessException("Alipay refund query failed: " + e.getErrMsg());
        }
    }

    private PaymentConfig.Alipay requireConfig() {
        PaymentConfig.Alipay config = paymentConfig.getAlipay();
        if (config == null
                || !StringUtils.hasText(config.getAppId())
                || !StringUtils.hasText(config.getPrivateKey())
                || !StringUtils.hasText(config.getPublicKey())
                || !StringUtils.hasText(config.getGatewayUrl())
                || !StringUtils.hasText(config.getNotifyUrl())
                || !StringUtils.hasText(config.getReturnUrl())) {
            throw new BusinessException("Alipay payment config is incomplete");
        }
        return config;
    }

    private AlipayClient getClient(PaymentConfig.Alipay config) {
        if (alipayClient == null) {
            synchronized (this) {
                if (alipayClient == null) {
                    alipayClient = new DefaultAlipayClient(
                            config.getGatewayUrl(),
                            config.getAppId(),
                            config.getPrivateKey(),
                            FORMAT,
                            CHARSET,
                            config.getPublicKey(),
                            SIGN_TYPE
                    );
                }
            }
        }
        return alipayClient;
    }

    private String mapRefundQueryStatus(boolean querySuccess, String refundStatus) {
        if (!querySuccess) {
            return RefundChannelStatusEnum.FAIL.name();
        }
        if (REFUND_SUCCESS_STATUS.equals(refundStatus)) {
            return RefundChannelStatusEnum.SUCCESS.name();
        }
        if (REFUND_FAIL_STATUS.equals(refundStatus)) {
            return RefundChannelStatusEnum.FAIL.name();
        }
        return RefundChannelStatusEnum.PROCESSING.name();
    }

    private String buildSubject(PaymentBill paymentBill) {
        return paymentBill.getBizType() + "-" + paymentBill.getBizNo();
    }

    private String buildBody(PaymentBill paymentBill) {
        return "payment bill " + paymentBill.getBillNo();
    }

    private String firstNonBlank(String... values) {
        for (String value : values) {
            if (StringUtils.hasText(value)) {
                return value;
            }
        }
        return "unknown";
    }
}


