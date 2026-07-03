package com.payment.rag.service.agent;

import org.springframework.http.codec.ServerSentEvent;

final class AgentSseEvents {

    private AgentSseEvents() {
    }

    static ServerSentEvent<String> event(String event, String data) {
        return ServerSentEvent.<String>builder()
                .event(event)
                .data(data)
                .build();
    }

    static ServerSentEvent<String> done() {
        return event("done", "{}");
    }

    static ServerSentEvent<String> error(String message) {
        return event("error", message);
    }
}
