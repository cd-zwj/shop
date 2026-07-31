package com.payment.service.delivery;

import com.fasterxml.jackson.databind.JsonNode;
import com.payment.entity.OrderDeliveryRecord;
import com.payment.util.JsonUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.Map;

/** 取货凭证 payload 的加密读写与历史格式兼容边界。 */
@Service
@RequiredArgsConstructor
public class PickupCodePayloadService {

    private final PickupCodeCryptoService cryptoService;

    public String createEncryptedPayload(Long tenantId, String orderNo, Long orderItemId,
                                         Long storeId, String pickupCode) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("pickupCodeCiphertext",
                cryptoService.encrypt(tenantId, orderNo, orderItemId, pickupCode));
        payload.put("storeId", storeId);
        return JsonUtils.toJson(payload);
    }

    public String readPickupCode(OrderDeliveryRecord record) {
        JsonNode payload = parse(record);
        JsonNode encrypted = payload.get("pickupCodeCiphertext");
        if (payload.has("pickupCodeCiphertext")) {
            String ciphertext = requireCiphertext(encrypted);
            return cryptoService.decrypt(
                    record.getTenantId(), record.getOrderNo(), record.getOrderItemId(), ciphertext);
        }
        JsonNode legacy = payload.get("pickupCode");
        String code = legacy != null && legacy.isTextual() ? legacy.asText() : null;
        if (code == null || !code.matches("\\d{8}")) {
            throw new IllegalStateException("取货凭证内容无效");
        }
        return code;
    }

    public boolean requiresRotation(OrderDeliveryRecord record) {
        JsonNode payload = parse(record);
        JsonNode encrypted = payload.get("pickupCodeCiphertext");
        if (!payload.has("pickupCodeCiphertext")) {
            readPickupCode(record);
            return true;
        }
        String ciphertext = requireCiphertext(encrypted);
        cryptoService.decrypt(
                record.getTenantId(), record.getOrderNo(), record.getOrderItemId(), ciphertext);
        return cryptoService.requiresRotation(ciphertext);
    }

    public String rotatePayload(OrderDeliveryRecord record) {
        return createEncryptedPayload(
                record.getTenantId(), record.getOrderNo(), record.getOrderItemId(),
                record.getStoreId(), readPickupCode(record));
    }

    private String requireCiphertext(JsonNode encrypted) {
        if (encrypted == null || !encrypted.isTextual() || encrypted.asText().isBlank()) {
            throw new IllegalStateException("取货凭证内容无效");
        }
        return encrypted.asText();
    }

    private JsonNode parse(OrderDeliveryRecord record) {
        if (record == null || record.getPayload() == null || record.getPayload().isBlank()) {
            throw new IllegalStateException("取货凭证内容无效");
        }
        try {
            JsonNode payload = JsonUtils.fromJsonTree(record.getPayload());
            if (payload == null || !payload.isObject()) {
                throw new IllegalArgumentException("payload is not an object");
            }
            return payload;
        } catch (IllegalArgumentException exception) {
            throw new IllegalStateException("取货凭证内容无效");
        }
    }
}
