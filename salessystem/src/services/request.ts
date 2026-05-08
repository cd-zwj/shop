import type { AxiosRequestConfig } from 'axios';
import { http } from './http';
import { ApiError, type ApiResponse } from '../types/api';

function unwrapResponse<T>(response: ApiResponse<T>): T {
  if (response.code !== 200) {
    throw new ApiError(response.message || '请求失败', response.code, undefined, response);
  }

  return response.data;
}

export async function request<T>(config: AxiosRequestConfig): Promise<T> {
  const { data } = await http.request<ApiResponse<T>>(config);
  return unwrapResponse(data);
}

export async function requestResponse<T>(config: AxiosRequestConfig): Promise<ApiResponse<T>> {
  const { data } = await http.request<ApiResponse<T>>(config);
  if (data.code !== 200) {
    throw new ApiError(data.message || '请求失败', data.code, undefined, data);
  }

  return data;
}

