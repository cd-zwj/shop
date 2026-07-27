package com.payment.util;

import cn.hutool.crypto.digest.DigestUtil;

import java.nio.charset.StandardCharsets;

/** 支付回调报文摘要与已签名幂等键工具。 */
public final class PaymentCallbackPayloadUtil {

    public static final int MAX_PAYLOAD_BYTES = 65_536;
    private PaymentCallbackPayloadUtil() {
    }

    public static String sha256(String rawBody) {
        return DigestUtil.sha256Hex(rawBody == null ? "" : rawBody);
    }

    public static int byteSize(String rawBody) {
        return rawBody == null ? 0 : rawBody.getBytes(StandardCharsets.UTF_8).length;
    }

    public static boolean isWithinLimit(String rawBody) {
        return byteSize(rawBody) <= MAX_PAYLOAD_BYTES;
    }

    public static String idempotencyKey(String channelCode,
                                        String signedBillNo,
                                        String signedRequestId,
                                        String rawBody) {
        String providerIdentity = signedRequestId == null || signedRequestId.isBlank()
                ? "PAYLOAD-" + sha256(rawBody)
                : signedRequestId;
        String material = (channelCode == null ? "" : channelCode)
                + "\n" + (signedBillNo == null ? "" : signedBillNo)
                + "\n" + providerIdentity;
        return "CALLBACK-" + sha256(material);
    }

    public static String auditMetadata(String rawBody) {
        return "sha256:" + sha256(rawBody) + ";bytes:" + byteSize(rawBody);
    }
}
