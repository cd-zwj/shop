package com.payment.rag.service.scenario;

import java.util.List;

public record ScenarioToolDescriptor(
        String name,
        String description,
        List<String> roles,
        List<String> requiredPermissions
) {
    public static ScenarioToolDescriptor of(String name, String description, List<String> roles, List<String> requiredPermissions) {
        return new ScenarioToolDescriptor(
                name,
                description,
                roles == null ? List.of() : List.copyOf(roles),
                requiredPermissions == null ? List.of() : List.copyOf(requiredPermissions)
        );
    }

    public boolean supports(String role, List<String> permissions) {
        String normalizedRole = role == null ? "" : role.toLowerCase();
        if (!roles.isEmpty() && !roles.contains(normalizedRole)) {
            return false;
        }
        return requiredPermissions.isEmpty() || (permissions != null && permissions.containsAll(requiredPermissions));
    }
}