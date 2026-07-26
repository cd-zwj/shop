package com.payment.constant;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MerchantPermissionTest {

    @Test
    void pickupClerkShouldManageOrdersButNotRefunds() {
        assertTrue(MerchantPermission.allows("PICKUP_CLERK", MerchantPermission.ORDER_MANAGE));
        assertFalse(MerchantPermission.allows("PICKUP_CLERK", MerchantPermission.REFUND_MANAGE));
        assertFalse(MerchantPermission.allows("PICKUP_CLERK", MerchantPermission.FINANCE_VIEW));
    }
}
