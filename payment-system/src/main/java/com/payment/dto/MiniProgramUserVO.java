package com.payment.dto;

import lombok.Data;

/**
 * 小程序用户信息VO
 */
@Data
public class MiniProgramUserVO {
    
    private Long id;
    
    private String nickname;
    
    private String avatar;
    
    private String phone;
    
    /**
     * JWT token
     */
    private String token;
}
