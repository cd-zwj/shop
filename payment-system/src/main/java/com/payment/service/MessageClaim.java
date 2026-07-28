package com.payment.service;

/** 消息抢占结果及本次处理令牌。 */
public record MessageClaim(MessageClaimResult result, String token) {

    public static MessageClaim acquired(String token) {
        return new MessageClaim(MessageClaimResult.ACQUIRED, token);
    }

    public static MessageClaim completed() {
        return new MessageClaim(MessageClaimResult.COMPLETED, null);
    }

    public static MessageClaim inProgress() {
        return new MessageClaim(MessageClaimResult.IN_PROGRESS, null);
    }
}
