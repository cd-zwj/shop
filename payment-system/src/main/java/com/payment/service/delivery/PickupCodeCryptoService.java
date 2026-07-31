package com.payment.service.delivery;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;

/** 取货码版本化 AES-256-GCM 加解密服务。 */
@Service
public class PickupCodeCryptoService {

    private static final String FORMAT_VERSION = "pc1";
    private static final int NONCE_LENGTH = 12;
    private static final int GCM_TAG_BITS = 128;

    private final String activeKeyId;
    private final Map<String, SecretKeySpec> keys;
    private final SecureRandom secureRandom = new SecureRandom();

    public PickupCodeCryptoService(
            @Value("${app.pickup-code.crypto.active-key-id:}") String activeKeyId,
            @Value("${app.pickup-code.crypto.keys:}") String encodedKeys) {
        this.activeKeyId = requireKeyId(activeKeyId);
        this.keys = parseKeys(encodedKeys);
        if (!keys.containsKey(this.activeKeyId)) {
            throw new IllegalStateException("取货码活跃加密密钥未配置");
        }
    }

    public String encrypt(Long tenantId, String orderNo, Long orderItemId, String pickupCode) {
        validateContext(tenantId, orderNo, orderItemId);
        if (pickupCode == null || !pickupCode.matches("\\d{8}")) {
            throw new IllegalArgumentException("取货码必须为 8 位数字");
        }
        byte[] nonce = new byte[NONCE_LENGTH];
        secureRandom.nextBytes(nonce);
        try {
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, keys.get(activeKeyId), new GCMParameterSpec(GCM_TAG_BITS, nonce));
            cipher.updateAAD(aad(tenantId, orderNo, orderItemId));
            byte[] encrypted = cipher.doFinal(pickupCode.getBytes(StandardCharsets.UTF_8));
            Base64.Encoder encoder = Base64.getUrlEncoder().withoutPadding();
            return FORMAT_VERSION + "." + activeKeyId + "."
                    + encoder.encodeToString(nonce) + "." + encoder.encodeToString(encrypted);
        } catch (GeneralSecurityException exception) {
            throw new IllegalStateException("取货码加密失败", exception);
        }
    }

    public String decrypt(Long tenantId, String orderNo, Long orderItemId, String ciphertext) {
        validateContext(tenantId, orderNo, orderItemId);
        try {
            String[] parts = ciphertext == null ? new String[0] : ciphertext.split("\\.", -1);
            if (parts.length != 4 || !FORMAT_VERSION.equals(parts[0])) {
                throw new IllegalArgumentException("invalid ciphertext format");
            }
            SecretKeySpec key = keys.get(parts[1]);
            if (key == null) {
                throw new IllegalArgumentException("unknown key id");
            }
            Base64.Decoder decoder = Base64.getUrlDecoder();
            byte[] nonce = decoder.decode(parts[2]);
            if (nonce.length != NONCE_LENGTH) {
                throw new IllegalArgumentException("invalid nonce");
            }
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, key, new GCMParameterSpec(GCM_TAG_BITS, nonce));
            cipher.updateAAD(aad(tenantId, orderNo, orderItemId));
            String pickupCode = new String(cipher.doFinal(decoder.decode(parts[3])), StandardCharsets.UTF_8);
            if (!pickupCode.matches("\\d{8}")) {
                throw new IllegalArgumentException("invalid plaintext");
            }
            return pickupCode;
        } catch (GeneralSecurityException | IllegalArgumentException exception) {
            throw new IllegalStateException("取货码密文校验失败");
        }
    }

    public boolean requiresRotation(String ciphertext) {
        String[] parts = ciphertext == null ? new String[0] : ciphertext.split("\\.", -1);
        return parts.length != 4 || !FORMAT_VERSION.equals(parts[0]) || !activeKeyId.equals(parts[1]);
    }

    private Map<String, SecretKeySpec> parseKeys(String encodedKeys) {
        Map<String, SecretKeySpec> parsed = new LinkedHashMap<>();
        if (encodedKeys == null || encodedKeys.isBlank()) {
            throw new IllegalStateException("取货码加密密钥不能为空");
        }
        for (String entry : encodedKeys.split(",")) {
            String[] pair = entry.trim().split("=", 2);
            if (pair.length != 2) {
                throw new IllegalStateException("取货码密钥配置格式错误");
            }
            String keyId = requireKeyId(pair[0]);
            byte[] keyBytes;
            try {
                keyBytes = Base64.getDecoder().decode(pair[1]);
            } catch (IllegalArgumentException exception) {
                throw new IllegalStateException("取货码密钥必须使用 Base64 编码");
            }
            if (keyBytes.length != 32) {
                throw new IllegalStateException("取货码密钥必须为 256 位");
            }
            if (parsed.putIfAbsent(keyId, new SecretKeySpec(keyBytes, "AES")) != null) {
                throw new IllegalStateException("取货码密钥 ID 重复");
            }
        }
        return Map.copyOf(parsed);
    }

    private String requireKeyId(String keyId) {
        String normalized = keyId == null ? "" : keyId.trim();
        if (!normalized.matches("[A-Za-z0-9_-]{1,32}")) {
            throw new IllegalStateException("取货码密钥 ID 不合法");
        }
        return normalized;
    }

    private void validateContext(Long tenantId, String orderNo, Long orderItemId) {
        if (tenantId == null || tenantId <= 0 || orderItemId == null || orderItemId <= 0
                || orderNo == null || orderNo.isBlank()) {
            throw new IllegalArgumentException("取货码加密上下文不完整");
        }
    }

    private byte[] aad(Long tenantId, String orderNo, Long orderItemId) {
        return ("pickup-code|" + tenantId + "|" + orderNo + "|" + orderItemId)
                .getBytes(StandardCharsets.UTF_8);
    }
}
