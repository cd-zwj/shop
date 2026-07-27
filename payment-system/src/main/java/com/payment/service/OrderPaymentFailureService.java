package com.payment.service;

/** Handles a terminal sales-order payment failure and releases reserved assets. */
public interface OrderPaymentFailureService {

    /**
     * @return true when this call claimed the pending order and released its assets
     */
    boolean failAndRelease(String orderNo, String reason);
}
