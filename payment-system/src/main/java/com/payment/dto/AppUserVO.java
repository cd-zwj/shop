package com.payment.dto;

import com.payment.entity.PlatformUser;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 平台用户安全视图，隐藏密码哈希等内部字段。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AppUserVO {
    private Long id;
    private String userNo;
    private String username;
    private String phone;
    private String email;
    private Integer status;
    private LocalDateTime createTime;

    public static AppUserVO toVO(PlatformUser user) {
        if (user == null) {
            return null;
        }
        return AppUserVO.builder()
                .id(user.getId())
                .userNo(user.getUserNo())
                .username(user.getUsername())
                .phone(user.getPhone())
                .email(user.getEmail())
                .status(user.getStatus())
                .createTime(user.getCreateTime())
                .build();
    }
}
