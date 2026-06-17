package com.payment.service.outbox;

import lombok.Getter;

@Getter
public class OutboxMessageCommand {

    private final String messagePrefix;
    private final String bizType;
    private final String bizNo;
    private final String exchangeName;
    private final String routingKey;
    private final Object messageBody;

    private OutboxMessageCommand(Builder builder) {
        this.messagePrefix = builder.messagePrefix;
        this.bizType = builder.bizType;
        this.bizNo = builder.bizNo;
        this.exchangeName = builder.exchangeName;
        this.routingKey = builder.routingKey;
        this.messageBody = builder.messageBody;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String messagePrefix;
        private String bizType;
        private String bizNo;
        private String exchangeName;
        private String routingKey;
        private Object messageBody;

        public Builder messagePrefix(String messagePrefix) {
            this.messagePrefix = messagePrefix;
            return this;
        }

        public Builder bizType(String bizType) {
            this.bizType = bizType;
            return this;
        }

        public Builder bizNo(String bizNo) {
            this.bizNo = bizNo;
            return this;
        }

        public Builder exchangeName(String exchangeName) {
            this.exchangeName = exchangeName;
            return this;
        }

        public Builder routingKey(String routingKey) {
            this.routingKey = routingKey;
            return this;
        }

        public Builder messageBody(Object messageBody) {
            this.messageBody = messageBody;
            return this;
        }

        public OutboxMessageCommand build() {
            return new OutboxMessageCommand(this);
        }
    }
}
