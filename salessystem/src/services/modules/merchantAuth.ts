import { request } from '../request';
import type { MerchantSession, PlatformLoginDTO } from '../../types/auth';

export const merchantAuthService = {
  login(payload: PlatformLoginDTO) {
    return request<MerchantSession>({
      url: '/v1/merchant/auth/login',
      method: 'post',
      data: payload,
      authRole: false,
    });
  },

  getCurrentSession() {
    return request<MerchantSession>({
      url: '/v1/merchant/auth/me',
      method: 'get',
      authRole: 'merchant',
    });
  },

  logout() {
    return request<void>({
      url: '/v1/merchant/auth/logout',
      method: 'post',
      authRole: 'merchant',
    });
  },
};

