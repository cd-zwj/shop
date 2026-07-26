package com.payment.constant;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * 商户员工本地角色权限矩阵。
 */
public enum MerchantPermission {
    DASHBOARD_VIEW("dashboard:view"),
    STORE_MANAGE("store:manage"),
    INVENTORY_MANAGE("inventory:manage"),
    PRODUCT_MANAGE("product:manage"),
    ORDER_MANAGE("order:manage"),
    REFUND_MANAGE("refund:manage"),
    FINANCE_VIEW("finance:view"),
    WITHDRAWAL_MANAGE("withdrawal:manage"),
    MARKETING_MANAGE("marketing:manage"),
    RULE_MANAGE("rule:manage"),
    EMPLOYEE_MANAGE("employee:manage"),
    AI_USE("ai:use");

    private static final Set<MerchantPermission> ALL_PERMISSIONS = EnumSet.allOf(MerchantPermission.class);
    private static final Map<MerchantRole, Set<MerchantPermission>> ROLE_PERMISSIONS = new EnumMap<>(MerchantRole.class);

    static {
        ROLE_PERMISSIONS.put(MerchantRole.OWNER, ALL_PERMISSIONS);
        ROLE_PERMISSIONS.put(MerchantRole.ADMIN, ALL_PERMISSIONS);
        ROLE_PERMISSIONS.put(MerchantRole.MANAGER, EnumSet.of(
                DASHBOARD_VIEW,
                STORE_MANAGE,
                INVENTORY_MANAGE,
                PRODUCT_MANAGE,
                ORDER_MANAGE,
                REFUND_MANAGE,
                MARKETING_MANAGE,
                RULE_MANAGE,
                AI_USE
        ));
        ROLE_PERMISSIONS.put(MerchantRole.OPERATOR, EnumSet.of(
                DASHBOARD_VIEW,
                INVENTORY_MANAGE,
                PRODUCT_MANAGE,
                ORDER_MANAGE,
                REFUND_MANAGE,
                MARKETING_MANAGE,
                AI_USE
        ));
        ROLE_PERMISSIONS.put(MerchantRole.PICKUP_CLERK, EnumSet.of(
                DASHBOARD_VIEW,
                ORDER_MANAGE,
                AI_USE
        ));
        ROLE_PERMISSIONS.put(MerchantRole.FINANCE, EnumSet.of(
                DASHBOARD_VIEW,
                FINANCE_VIEW,
                WITHDRAWAL_MANAGE,
                AI_USE
        ));
    }

    private final String code;

    MerchantPermission(String code) {
        this.code = code;
    }

    public String getCode() {
        return code;
    }

    public static boolean allows(String rawRole, MerchantPermission permission) {
        if (permission == null) {
            return true;
        }
        MerchantRole role = MerchantRole.from(rawRole);
        Set<MerchantPermission> permissions = role == null ? null : ROLE_PERMISSIONS.get(role);
        return permissions == null ? permission == DASHBOARD_VIEW : permissions.contains(permission);
    }

    private enum MerchantRole {
        OWNER,
        ADMIN,
        MANAGER,
        OPERATOR,
        PICKUP_CLERK,
        FINANCE;

        static MerchantRole from(String rawRole) {
            if (rawRole == null || rawRole.isBlank()) {
                return null;
            }
            try {
                return MerchantRole.valueOf(rawRole.trim().toUpperCase(Locale.ROOT));
            } catch (IllegalArgumentException ex) {
                return null;
            }
        }
    }
}
