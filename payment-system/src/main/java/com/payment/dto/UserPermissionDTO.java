package com.payment.dto;

import lombok.Data;
import java.util.List;

@Data
public class UserPermissionDTO {
    private List<Long> permissionIds;
}
