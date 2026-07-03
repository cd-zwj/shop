package com.payment.rag.Config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "chunking.hierarchy")
@Data
public class HierarchyConfig {
    private boolean enabled = true;
    private int midChildCount = 6;
    private int summaryCandidateTopK = 12;
    private int midRerankTopK = 4;
    private int leafRerankTopK = 8;
    private boolean rebuildOnStartup = true;
    private int summaryTimeoutSeconds = 20;
    private int summaryFailureCooldownSeconds = 120;
}
