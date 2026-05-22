package com.payment.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * AI工具类
 * AI 功能暂时关闭，保留占位类避免影响现有注入关系。
 */
@Slf4j
@Component("aiTools")
public class AiTools {
    public void logDisabled(String scene) {
        log.warn("AI 功能已临时关闭: {}", scene);
    }
}
