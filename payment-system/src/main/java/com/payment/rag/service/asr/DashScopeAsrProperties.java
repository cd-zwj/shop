package com.payment.rag.service.asr;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "asr.dashscope")
@Data
public class DashScopeAsrProperties {

    private boolean enabled = true;
    private String apiKey;
    private String model = "paraformer-realtime-v2";
    private String format = "pcm";
    private int sampleRate = 16000;
}
