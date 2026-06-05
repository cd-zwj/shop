package com.payment.config;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateTimeDeserializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer;
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.format.DateTimeFormatter;

/**
 * 全局 Jackson 序列化配置。
 * <p>
 * 1. 将所有 {@link java.time.LocalDateTime} 字段统一格式化为 ISO-8601 字符串，
 *    例如 {@code 2024-03-10T14:30:00}，与前端约定一致。
 * 2. 将所有 {@link java.math.BigDecimal} 字段统一序列化为纯数字（小数点后保留两位），
 *    避免科学计数法或字符串格式，前端始终收到 {@code 100.00} 而非 {@code "100.00"} 或 {@code 1E+2}。
 */
@Configuration
public class JacksonConfig {

    private static final String DATE_TIME_PATTERN = "yyyy-MM-dd'T'HH:mm:ss";

    /**
     * 统一日期时间序列化格式。
     */
    @Bean
    public Jackson2ObjectMapperBuilderCustomizer jacksonDateTimeCustomizer() {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern(DATE_TIME_PATTERN);
        return builder -> {
            builder.serializers(new LocalDateTimeSerializer(formatter));
            builder.deserializers(new LocalDateTimeDeserializer(formatter));
        };
    }

    /**
     * 全局 BigDecimal 序列化配置。
     * <ul>
     *   <li>{@link SerializationFeature#WRITE_BIGDECIMAL_AS_PLAIN} — 禁止科学计数法，
     *       {@code 1E+2} 会输出为 {@code 100}。</li>
     *   <li>自定义 {@link PlainBigDecimalSerializer} — 将所有 BigDecimal 规范化为小数点后两位，
     *       确保 {@code 100} 输出为 {@code 100.00}，与数据库 DECIMAL(20,2) 一致。</li>
     * </ul>
     */
    @Bean
    public Jackson2ObjectMapperBuilderCustomizer jacksonBigDecimalCustomizer() {
        return builder -> {
            builder.featuresToDisable(SerializationFeature.WRITE_BIGDECIMAL_AS_PLAIN);
            builder.modules(new SimpleModule("BigDecimalModule") {
                @Override
                public void setupModule(SetupContext context) {
                    super.setupModule(context);
                    addSerializer(BigDecimal.class, new PlainBigDecimalSerializer());
                }
            });
        };
    }

    /**
     * BigDecimal 序列化器：输出纯数字，小数点后保留两位，禁止科学计数法。
     * <p>
     * 保证前端始终收到数字类型 {@code 100.00}，而非字符串 {@code "100.00"}。
     */
    private static class PlainBigDecimalSerializer extends JsonSerializer<BigDecimal> {

        @Override
        public void serialize(BigDecimal value, JsonGenerator gen, SerializerProvider provider)
                throws IOException {
            if (value == null) {
                gen.writeNull();
            } else {
                gen.writeNumber(value.setScale(2, RoundingMode.HALF_UP));
            }
        }
    }
}
