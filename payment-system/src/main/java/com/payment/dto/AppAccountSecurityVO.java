package com.payment.dto;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * 用户端账号安全视图对象，用于返回手机号、邮箱、密码和第三方绑定状态。
 */
@Data
public class AppAccountSecurityVO {
    private SecurityBindingVO phone;
    private SecurityBindingVO email;
    private PasswordSecurityVO password;
    private List<ThirdPartyBindingVO> thirdPartyBindings = new ArrayList<>();

    @Data
    public static class SecurityBindingVO {
        private Boolean bound;
        private String maskedValue;
    }

    @Data
    public static class PasswordSecurityVO {
        private Boolean set;
    }

    @Data
    public static class ThirdPartyBindingVO {
        private Long providerId;
        private String providerCode;
        private String providerName;
        private Boolean bound;
    }
}
