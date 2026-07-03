package com.payment.dto;

import com.payment.entity.PlatformUser;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 平台用户视图对象，用于向前端返回用户基本信息（隐藏密码哈希等内部字段）。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AppUserVO {
    /** 用户 ID */
    private Long id;
    /** 用户编号（业务唯一标识） */
    private String userNo;
    /** 用户名 */
    private String username;
    /** 手机号 */
    private String phone;
    /** 邮箱 */
    private String email;
    /** 账号状态（0-禁用, 1-正常） */
    private Integer status;
    /** 注册时间 */
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
