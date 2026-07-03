import axios, { AxiosError, type InternalAxiosRequestConfig } from 'axios';
import { API_BASE_URL } from '../config/env';
import type { AuthRole } from '../types/auth';
import { ApiError, type ApiResponse } from '../types/api';
import { getCurrentAuthRole } from '../utils/authSession';
import { getToken } from '../utils/token';

// 401 清除 token 事件名称，AuthContext 监听此事件完成本地状态清理
export const AUTH_TOKEN_CLEAR_EVENT = 'salessystem:auth:clear-tokens';

export function formatAuthToken(token: string): string {
  return token.startsWith('Bearer ') ? token : `Bearer ${token}`;
}

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

function dispatchAuthClear(config: InternalAxiosRequestConfig) {
  const role = resolveAuthRole(config);
  if (role) {
    window.dispatchEvent(new CustomEvent(AUTH_TOKEN_CLEAR_EVENT, { detail: { role } }));
  }
}

export const http = axios.create({
  baseURL: API_BASE_URL,
  timeout: 15000,
});

// 请求拦截器：自动注入 Sa-Token token。
http.interceptors.request.use((config) => {
  const nextConfig = config;
  const role = resolveAuthRole(nextConfig);
  const token = role ? getToken(role) : null;

  if (token) {
    const authToken = formatAuthToken(token);
    nextConfig.headers.set('Authorization', authToken);
    nextConfig.headers.set('satoken', authToken);
  }

  return nextConfig;
});

// 响应拦截器：统一错误处理，401 通知 AuthContext 清理认证状态
http.interceptors.response.use(
  (response) => response,
  (error: AxiosError<ApiResponse<unknown>>) => {
    if (error.response) {
      const { status, data } = error.response;

      if (status === 401) {
        dispatchAuthClear(error.config as InternalAxiosRequestConfig);
        return Promise.reject(new ApiError('登录已过期，请重新登录', 401, 401, data));
      }

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
