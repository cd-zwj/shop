import { request } from '../request';
import type { AdminSession, PlatformLoginDTO } from '../../types/auth';

export const adminAuthService = {
  login(payload: PlatformLoginDTO) {
    return request<string>({
      url: '/v1/admin/auth/login',
      method: 'post',
      data: payload,
      authRole: false,
    });
  },

  getCurrentSession() {
    return request<AdminSession>({
      url: '/v1/admin/auth/session',
      method: 'get',
      authRole: 'admin',
    });
  },

  logout() {
    return request<void>({
      url: '/v1/admin/auth/logout',
      method: 'post',
      authRole: 'admin',
    });
  },
};

