package com.payment.enums;

public enum WalletStrategyEnum {
    NO_WALLET,
    UNIFIED_ONLY,
    MERCHANT_ONLY,
    MERCHANT_THEN_UNIFIED,
    UNIFIED_THEN_MERCHANT,
    CUSTOM_SPLIT
}
