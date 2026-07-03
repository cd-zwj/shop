package com.payment.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/**
 * JSON 序列化/反序列化工具类。
 * <p>
 * 基于 Jackson ObjectMapper 实现，优先使用 Spring 容器中的 ObjectMapper Bean
 * （保持与全局 JacksonConfig 一致的序列化策略），容器不可用时回退到默认实例。
 * </p>
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class JsonUtils {

    /** 回退用的默认 ObjectMapper（Spring 容器不可用时使用） */
    private static final ObjectMapper FALLBACK_MAPPER = new ObjectMapper();

    /**
     * 获取可用的 ObjectMapper 实例。
     * <p>
     * 优先从 Spring 容器获取（继承 JacksonConfig 的序列化配置），
     * 获取失败时回退到默认实例。
     *
     * @return ObjectMapper 实例
     */
    private static ObjectMapper objectMapper() {
        try {
            ObjectMapper springMapper = SpringContextUtil.getBean(ObjectMapper.class);
            return springMapper == null ? FALLBACK_MAPPER : springMapper;
        } catch (Exception ignored) {
            return FALLBACK_MAPPER;
        }
    }

    /**
     * 将对象序列化为 JSON 字符串。
     *
     * @param value 要序列化的对象
     * @return JSON 字符串
     * @throws IllegalArgumentException 序列化失败时抛出
     */
    public static String toJson(Object value) {
        try {
            return objectMapper().writeValueAsString(value);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Failed to serialize object to JSON", e);
        }
    }

    /**
     * 将 JSON 字符串反序列化为指定类型的对象。
     *
     * @param <T>   目标类型
     * @param json  JSON 字符串
     * @param clazz 目标类
     * @return 反序列化后的对象
     * @throws IllegalArgumentException 反序列化失败时抛出
     */
    public static <T> T fromJson(String json, Class<T> clazz) {
        try {
            return objectMapper().readValue(json, clazz);
        } catch (Exception e) {
            throw new IllegalArgumentException("Failed to deserialize JSON", e);
        }
    }

    /**
     * 将 JSON 字符串反序列化为带泛型类型的对象。
     *
     * @param <T>          目标类型
     * @param json         JSON 字符串
     * @param typeReference 类型引用（如 {@code new TypeReference<List<String>>(){}}）
     * @return 反序列化后的对象
     * @throws IllegalArgumentException 反序列化失败时抛出
     */
    public static <T> T fromJson(String json, TypeReference<T> typeReference) {
        try {
            return objectMapper().readValue(json, typeReference);
        } catch (Exception e) {
            throw new IllegalArgumentException("Failed to deserialize JSON", e);
        }
    }

    /**
     * 将 JSON 字符串解析为 JsonNode 树结构。
     *
     * @param json JSON 字符串
     * @return JsonNode 根节点
     * @throws IllegalArgumentException 解析失败时抛出
     */
    public static JsonNode fromJsonTree(String json) {
        try {
            return objectMapper().readTree(json);
        } catch (Exception e) {
            throw new IllegalArgumentException("Failed to parse JSON tree", e);
        }
    }
}
