package com.payment.dto;

import lombok.Data;
import jakarta.validation.constraints.NotBlank;

/**
 * 微信小程序登录DTO
 */
@Data
public class WechatLoginDTO {
    
    @NotBlank(message = "微信code不能为空")
    private String code;
    
    /**
     * 用户昵称
     */
    private String nickname;
    
    /**
     * 用户头像
     */
    private String avatar;
    
    /**
     * 手机号
     */
    private String phone;
}
