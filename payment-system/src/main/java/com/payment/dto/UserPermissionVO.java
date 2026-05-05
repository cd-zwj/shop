package com.payment.dto;

import lombok.Data;
import java.util.List;

@Data
public class UserPermissionVO {
    private Long userId;
    private String username;
    private List<String> rolePermissions;
    private List<String> extraPermissions;
    private List<String> allPermissions;
}
