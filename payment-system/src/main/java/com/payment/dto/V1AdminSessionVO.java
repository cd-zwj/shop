package com.payment.dto;

import lombok.Data;

import java.util.List;

@Data
public class V1AdminSessionVO {

    private Long userId;
    private String username;
    private String nickname;
    private Integer userType;
    private String role;
    private String scope;
    private List<String> roles;
    private List<String> permissions;
}
