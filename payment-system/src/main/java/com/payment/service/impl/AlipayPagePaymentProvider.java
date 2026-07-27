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

/**
 * 支付宝网页支付提供商实现。
 * <p>
 * 基于支付宝 V3 SDK 实现 {@link PaymentProvider} 接口，提供以下能力：
 * <ul>
 *   <li>创建电脑网站支付（FAST_INSTANT_TRADE_PAY），返回支付页面 HTML</li>
 *   <li>验证支付宝异步回调签名（RSA2），并校验交易状态</li>
 *   <li>主动查询支付宝订单支付状态</li>
 *   <li>提交退款申请并查询退款状态</li>
 * </ul>
 * AlipayClient 采用双重检查锁延迟初始化，避免配置未就绪时的启动失败。
 */
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

    /** 返回支付宝网页支付渠道编码 {@code ALIPAY_PAGE}。 */
    @Override
    public String getChannelCode() {
        return PaymentChannelCodeEnum.ALIPAY_PAGE.name();
    }

    /**
     * 创建支付宝网页支付。
     * <p>
     * 构造 {@code AlipayTradePagePayRequest} 并调用支付宝 pageExecute 获取支付页面 HTML，
     * 有效期 30 分钟。支付金额精确到分（HALF_UP）。
     *
     * @param paymentBill 支付账单实体，包含账单号、金额、业务类型等信息
     * @return 包含支付链接（payUrl）的响应 DTO
     * @throws BusinessException 调用支付宝接口失败时抛出
     */
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

    /**
     * 验证支付宝异步回调的合法性。
     * <p>
     * 使用 RSA2 公钥验证回调参数签名。交易结果由支付账单服务单独判断，
     * 因而有效的 {@code TRADE_CLOSED} 等非成功通知也必须通过这里。
     *
     * @param callbackDTO 回调数据，包含原始请求体和账单号
     * @return 签名有效时返回 {@code true}
     */
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

            return true;
        } catch (Exception e) {
            log.error("Failed to verify alipay callback, billNo={}", callbackDTO.getBillNo(), e);
            return false;
        }
    }

    /**
     * 主动向支付宝查询订单支付状态。
     * <p>
     * 根据外部订单号（outTradeNo）或支付宝交易号（tradeNo）发起查询，
     * 返回是否已支付、原始状态、买家信息等。
     *
     * @param paymentBill 支付账单实体
     * @return 支付查询结果，包含成功标志、交易号、支付状态等
     * @throws BusinessException 查询接口调用失败时抛出
     */
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

    /** 返回 {@code true}，支付宝网页支付支持退款。 */
    @Override
    public boolean supportsRefund() {
        return true;
    }

    /**
     * 向支付宝提交退款请求。
     * <p>
     * 支持部分退款和全额退款，退款金额精确到分。使用退款单号作为唯一退款标识。
     *
     * @param paymentBill 原支付账单
     * @param requestDTO  退款请求参数，包含退款金额、原因、退款单号
     * @return 退款提交结果，包含渠道状态（SUCCESS / FAIL / PROCESSING）
     * @throws BusinessException 退款接口调用失败时抛出
     */
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

    /**
     * 查询支付宝退款状态。
     * <p>
     * 根据退款单号查询退款进度，返回退款成功、失败或处理中的状态。
     *
     * @param refundRecord 退款记录实体
     * @return 退款查询结果，包含渠道退款状态和第三方退款单号
     * @throws BusinessException 查询接口调用失败时抛出
     */
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

    /**
     * 校验并返回支付宝配置，任一必要字段缺失时抛出异常。
     */
    private PaymentConfig.Alipay requireConfig() {
        PaymentConfig.Alipay config = paymentConfig.getAlipay();
        if (config == null
                || !StringUtils.hasText(config.getAppId())
                || !StringUtils.hasText(config.getPrivateKey())
                || !StringUtils.hasText(config.getPublicKey())
                || !StringUtils.hasText(config.getGatewayUrl())
                || !StringUtils.hasText(config.getNotifyUrl())
                || !StringUtils.hasText(config.getReturnUrl())
                || !StringUtils.hasText(config.getSellerId())) {
            throw new BusinessException("Alipay payment config is incomplete (seller-id required)");
        }
        return config;
    }

    /**
     * 获取或延迟初始化 AlipayClient 实例（双重检查锁，线程安全）。
     */
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

    /**
     * 将支付宝退款查询的原始状态映射为系统退款渠道状态枚举。
     */
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

    /** 构造支付订单标题，格式：{bizType}-{bizNo}。 */
    private String buildSubject(PaymentBill paymentBill) {
        return paymentBill.getBizType() + "-" + paymentBill.getBizNo();
    }

    /** 构造支付订单描述信息。 */
    private String buildBody(PaymentBill paymentBill) {
        return "payment bill " + paymentBill.getBillNo();
    }

    /** 返回参数列表中第一个非空值，全部为空时返回 "unknown"。 */
    private String firstNonBlank(String... values) {
        for (String value : values) {
            if (StringUtils.hasText(value)) {
                return value;
            }
        }
        return "unknown";
    }
}


