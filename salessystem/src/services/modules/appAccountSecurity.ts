import { request } from '../request';

/**
 * 账号安全视图对象
 */
export interface AccountSecurityVO {
  phone: SecurityBindingVO;
  email: SecurityBindingVO;
  password: PasswordSecurityVO;
  thirdPartyBindings: ThirdPartyBindingVO[];
}

export interface SecurityBindingVO {
  bound: boolean;
  maskedValue: string | null;
}

export interface PasswordSecurityVO {
  set: boolean;
}

export interface ThirdPartyBindingVO {
  providerId: number;
  providerCode: string;
  providerName: string;
  bound: boolean;
}

/**
 * 修改密码请求体
 */
export interface ChangePasswordPayload {
  oldPassword: string;
  newPassword: string;
}

export const appAccountSecurityService = {
  /**
   * 获取账号安全摘要（手机号、邮箱、密码状态、第三方绑定）
   *
   * 注意：后端 Controller 尚未暴露此接口，页面已做好兼容——
   * 若接口返回 404 则静默降级，不影响页面其他功能。
   */
  getSecuritySummary() {
    return request<AccountSecurityVO>({
      url: '/v1/app/account-security/summary',
      method: 'get',
      authRole: 'user',
    });
  },

  /**
   * 登录态内修改密码。
   *
   * 注意：后端 Controller 尚未暴露此接口，调用会 404。
   * 页面已做降级处理，显示"修改密码功能开发中"提示。
   */
  changePassword(payload: ChangePasswordPayload) {
    return request<void>({
      url: '/v1/app/account-security/change-password',
      method: 'post',
      data: payload,
      authRole: 'user',
    });
  },
};
