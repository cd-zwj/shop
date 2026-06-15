import { request } from '../request';
import type {
  LoginCaptchaVO,
  PlatformEmailSendCodeDTO,
  PlatformLoginDTO,
  PlatformRegisterDTO,
  PlatformResetPasswordDTO,
  PlatformUser,
  SmsLoginDTO,
  SmsSendCodeDTO,
} from '../../types/auth';

export const appAuthService = {
  getCaptcha() {
    return request<LoginCaptchaVO>({
      url: '/v1/auth/captcha',
      method: 'get',
      authRole: false,
    });
  },

  register(payload: PlatformRegisterDTO) {
    return request<PlatformUser>({
      url: '/v1/app/auth/register',
      method: 'post',
      data: payload,
      authRole: false,
    });
  },

  loginByPassword(payload: PlatformLoginDTO) {
    return request<string>({
      url: '/v1/app/auth/login/password',
      method: 'post',
      data: payload,
      authRole: false,
    });
  },

  sendSmsCode(payload: SmsSendCodeDTO) {
    return request<void>({
      url: '/v1/app/auth/sms/send-code',
      method: 'post',
      data: payload,
      authRole: false,
    });
  },

  sendPasswordResetCode(payload: PlatformEmailSendCodeDTO) {
    return request<void>({
      url: '/v1/app/auth/password/reset/send-code',
      method: 'post',
      data: payload,
      authRole: false,
    });
  },

  resetPassword(payload: PlatformResetPasswordDTO) {
    return request<void>({
      url: '/v1/app/auth/password/reset/verify',
      method: 'post',
      data: payload,
      authRole: false,
    });
  },

  loginBySms(payload: SmsLoginDTO) {
    return request<string>({
      url: '/v1/app/auth/login/sms',
      method: 'post',
      data: payload,
      authRole: false,
    });
  },

  loginByThirdParty(payload: PlatformLoginDTO) {
    return request<string>({
      url: '/v1/app/auth/login/third-party',
      method: 'post',
      data: payload,
      authRole: false,
    });
  },

  logout() {
    return request<void>({
      url: '/v1/app/auth/logout',
      method: 'post',
      authRole: 'user',
    });
  },
};
