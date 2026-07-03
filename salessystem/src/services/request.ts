import type { AxiosRequestConfig } from 'axios';
import { http } from './http';
import { ApiError, type ApiResponse } from '../types/api';
import { getCurrentAuthRole } from '../utils/authSession';
import { AUTH_TOKEN_CLEAR_EVENT } from './http';

function dispatchAuthClear(config: AxiosRequestConfig) {
  if (config.authRole === false) {
    return;
  }

  const role = config.authRole ?? getCurrentAuthRole();
  if (role) {
    window.dispatchEvent(new CustomEvent(AUTH_TOKEN_CLEAR_EVENT, { detail: { role } }));
  }
}

function unwrapResponse<T>(response: ApiResponse<T>, config: AxiosRequestConfig): T {
  if (response.code === 401) {
    dispatchAuthClear(config);
    throw new ApiError(response.message || '登录已过期，请重新登录', 401, 401, response);
  }

  if (response.code !== 200) {
    throw new ApiError(response.message || '请求失败', response.code, undefined, response);
  }

  return response.data;
}

export async function request<T>(config: AxiosRequestConfig): Promise<T> {
  const { data } = await http.request<ApiResponse<T>>(config);
  return unwrapResponse(data, config);
}

export async function requestResponse<T>(config: AxiosRequestConfig): Promise<ApiResponse<T>> {
  const { data } = await http.request<ApiResponse<T>>(config);
  if (data.code === 401) {
    dispatchAuthClear(config);
    throw new ApiError(data.message || '登录已过期，请重新登录', 401, 401, data);
  }

  if (data.code !== 200) {
    throw new ApiError(data.message || '请求失败', data.code, undefined, data);
  }

  return data;
}

