package com.payment.rag.model.dto;

public enum AgentMode {
    AUTO,
    REACT,
    PLAN_EXECUTE;

    public static AgentMode defaultIfNull(AgentMode mode) {
        return mode == null ? AUTO : mode;
    }
}
