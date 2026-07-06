package com.payment.constant;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MerchantPermissionTest {

    @Test
    void cashierShouldManageOrdersButNotRefunds() {
        assertTrue(MerchantPermission.allows("CASHIER", MerchantPermission.ORDER_MANAGE));
        assertFalse(MerchantPermission.allows("CASHIER", MerchantPermission.REFUND_MANAGE));
        assertFalse(MerchantPermission.allows("CASHIER", MerchantPermission.FINANCE_VIEW));
    }
}
