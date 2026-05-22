package com.payment.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class JsonUtils {

    private static final ObjectMapper FALLBACK_MAPPER = new ObjectMapper();

    private static ObjectMapper objectMapper() {
        try {
            ObjectMapper springMapper = SpringContextUtil.getBean(ObjectMapper.class);
            return springMapper == null ? FALLBACK_MAPPER : springMapper;
        } catch (Exception ignored) {
            return FALLBACK_MAPPER;
        }
    }

    public static String toJson(Object value) {
        try {
            return objectMapper().writeValueAsString(value);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Failed to serialize object to JSON", e);
        }
    }

    public static <T> T fromJson(String json, Class<T> clazz) {
        try {
            return objectMapper().readValue(json, clazz);
        } catch (Exception e) {
            throw new IllegalArgumentException("Failed to deserialize JSON", e);
        }
    }

    public static <T> T fromJson(String json, TypeReference<T> typeReference) {
        try {
            return objectMapper().readValue(json, typeReference);
        } catch (Exception e) {
            throw new IllegalArgumentException("Failed to deserialize JSON", e);
        }
    }

    public static JsonNode fromJsonTree(String json) {
        try {
            return objectMapper().readTree(json);
        } catch (Exception e) {
            throw new IllegalArgumentException("Failed to parse JSON tree", e);
        }
    }
}
