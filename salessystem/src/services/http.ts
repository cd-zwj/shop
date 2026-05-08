import axios, { AxiosError, type InternalAxiosRequestConfig } from 'axios';
import { API_BASE_URL } from '../config/env';
import type { AuthRole } from '../types/auth';
import { ApiError, type ApiResponse } from '../types/api';
import { getCurrentAuthRole } from '../utils/authSession';
import { getToken } from '../utils/token';

declare module 'axios' {
  interface AxiosRequestConfig {
    authRole?: AuthRole | false;
    skipErrorToast?: boolean;
  }
}

function resolveAuthRole(config: InternalAxiosRequestConfig): AuthRole | null {
  if (config.authRole === false) {
    return null;
  }

  return config.authRole ?? getCurrentAuthRole();
}

export const http = axios.create({
  baseURL: API_BASE_URL,
  timeout: 15000,
});

http.interceptors.request.use((config) => {
  const nextConfig = config;
  const role = resolveAuthRole(nextConfig);
  const token = role ? getToken(role) : null;

  if (token) {
    nextConfig.headers.set('Authorization', token);
  }

  return nextConfig;
});

http.interceptors.response.use(
  (response) => response,
  (error: AxiosError<ApiResponse<unknown>>) => {
    if (error.response) {
      const { status, data } = error.response;
      const message =
        data?.message || (status === 403 ? '当前账号没有访问权限' : '请求失败，请稍后重试');
      return Promise.reject(new ApiError(message, data?.code ?? status, status, data));
    }

    if (error.request) {
      return Promise.reject(new ApiError('网络连接失败，请检查后端服务或网络配置'));
    }

    return Promise.reject(new ApiError(error.message || '请求初始化失败'));
  },
);

