package com.payment.rag.Config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Slf4j
@Component
public class DateTimeTools {
    /**
     * 1.定义 function call（tool call）
     * 2. returnDirect
     *    true = tool直接返回不走大模型，直接给客户
     *    false = 默认值，拿到tool返回的结果，给大模型，最后由大模型回复
     */
    @Tool(description = "获取当前系统时间和日期。当用户询问现在时间、当前日期、今天几号时使用", returnDirect = false)
    public String getCurrentTime() {
        log.debug("获取当前时间");
        return LocalDateTime.now().toString();
    }
}