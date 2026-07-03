package com.payment.rag.service.asr;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.ArrayList;
import java.util.List;

@Configuration
@ConfigurationProperties(prefix = "asr")
@Data
public class AsrFallbackProperties {

    private List<String> providerOrder = new ArrayList<>(List.of("aliyun-nls", "dashscope-paraformer"));
}
