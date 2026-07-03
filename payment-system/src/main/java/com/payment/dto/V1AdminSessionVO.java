package com.payment.dto;

import lombok.Data;

import java.util.List;

/**
 * 平台管理端登录成功后的会话信息视图对象。
 */
@Data
public class V1AdminSessionVO {

    /** 用户ID */
    private Long userId;

    /** 用户名 */
    private String username;

    /** 用户昵称 */
    private String nickname;

    /** 用户类型（区分管理员、商户等角色类型） */
    private Integer userType;

    /** 主角色标识 */
    private String role;

    /** 权限范围 */
    private String scope;

    /** 角色列表（一个管理员可拥有多个角色） */
    private List<String> roles;

    /** 权限标识列表（从角色继承的全部权限） */
    private List<String> permissions;
}
