import { request } from '../request';
import type { PlatformUser } from '../../types/auth';

export const appUserService = {
  getCurrentUser() {
    return request<PlatformUser>({
      url: '/v1/app/users/me',
      method: 'get',
      authRole: 'user',
    });
  },
};

