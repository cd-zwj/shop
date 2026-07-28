package com.payment.service;

/** 原子消息抢占结果。 */
public enum MessageClaimResult {
    ACQUIRED,
    COMPLETED,
    IN_PROGRESS
}
