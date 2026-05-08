import { request } from '../request';
import type { PageResult } from '../../types/api';
import type {
  AdminPermissionCatalog,
  AdminPlatformUser,
  AdminUserPermissionDetail,
} from '../../types/admin';

export const adminUserService = {
  listUsers(params: {
    current?: number;
    size?: number;
    keyword?: string;
    status?: number;
  } = {}) {
    return request<PageResult<AdminPlatformUser>>({
      url: '/v1/admin/users',
      method: 'get',
      params: {
        current: params.current ?? 1,
        size: params.size ?? 10,
        keyword: params.keyword || undefined,
        status: params.status,
      },
      authRole: 'admin',
    });
  },

  getUserDetail(userId: number) {
    return request<AdminPlatformUser>({
      url: `/v1/admin/users/${userId}`,
      method: 'get',
      authRole: 'admin',
    });
  },

  enableUser(userId: number) {
    return request<void>({
      url: `/v1/admin/users/${userId}/enable`,
      method: 'put',
      authRole: 'admin',
    });
  },

  disableUser(userId: number) {
    return request<void>({
      url: `/v1/admin/users/${userId}/disable`,
      method: 'put',
      authRole: 'admin',
    });
  },

  listPermissions() {
    return request<AdminPermissionCatalog>({
      url: '/v1/admin/permissions',
      method: 'get',
      authRole: 'admin',
    });
  },

  getUserPermissions(userId: number) {
    return request<AdminUserPermissionDetail>({
      url: `/v1/admin/users/${userId}/permissions`,
      method: 'get',
      authRole: 'admin',
    });
  },

  setUserPermissions(userId: number, permissionIds: number[]) {
    return request<void>({
      url: `/v1/admin/users/${userId}/permissions`,
      method: 'put',
      data: { permissionIds },
      authRole: 'admin',
    });
  },

  removeUserPermission(userId: number, permissionId: number) {
    return request<void>({
      url: `/v1/admin/users/${userId}/permissions/${permissionId}`,
      method: 'delete',
      authRole: 'admin',
    });
  },
};
