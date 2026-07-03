package com.payment.dto;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * 用户端账号安全视图对象，用于返回手机号、邮箱、密码和第三方绑定状态。
 */
@Data
public class AppAccountSecurityVO {
    /** 手机号绑定信息 */
    private SecurityBindingVO phone;
    /** 邮箱绑定信息 */
    private SecurityBindingVO email;
    /** 密码设置状态 */
    private PasswordSecurityVO password;
    /** 第三方登录绑定列表 */
    private List<ThirdPartyBindingVO> thirdPartyBindings = new ArrayList<>();

    /**
     * 安全绑定信息（手机号/邮箱），包含绑定状态和脱敏值。
     */
    @Data
    public static class SecurityBindingVO {
        /** 是否已绑定 */
        private Boolean bound;
        /** 脱敏后的值（如 138****1234） */
        private String maskedValue;
    }

    /**
     * 密码安全信息，标识用户是否已设置密码。
     */
    @Data
    public static class PasswordSecurityVO {
        /** 是否已设置密码 */
        private Boolean set;
    }

    /**
     * 第三方登录绑定信息。
     */
    @Data
    public static class ThirdPartyBindingVO {
        /** 认证提供方 ID */
        private Long providerId;
        /** 认证提供方编码（如 WECHAT, GITHUB） */
        private String providerCode;
        /** 认证提供方名称 */
        private String providerName;
        /** 是否已绑定 */
        private Boolean bound;
    }
}
