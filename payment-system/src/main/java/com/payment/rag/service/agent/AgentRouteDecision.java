package com.payment.rag.service.agent;

import com.payment.rag.model.dto.AgentMode;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AgentRouteDecision {
    private AgentMode mode;
    private double confidence;
    private String reason;
    private String source;

    public static AgentRouteDecision of(AgentMode mode, double confidence, String reason, String source) {
        return new AgentRouteDecision(mode, confidence, reason, source);
    }

    public static AgentRouteDecision reactFallback(String reason) {
        return of(AgentMode.REACT, 0.0, reason, "fallback");
    }
}
