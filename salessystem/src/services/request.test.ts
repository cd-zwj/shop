import { describe, it, expect, vi, beforeEach } from 'vitest';

// Mock http 模块 — 使用 vi.hoisted 确保在 vi.mock 工厂之前初始化
const { mockHttp } = vi.hoisted(() => ({
  mockHttp: { request: vi.fn() },
}));
vi.mock('./http', () => ({
  http: mockHttp,
}));

import { request, requestResponse } from './request';
import { ApiError } from '../types/api';

beforeEach(() => {
  vi.clearAllMocks();
});

describe('request', () => {
  it('响应 code 为 200 时应返回 data 字段', async () => {
    // Arrange
    mockHttp.request.mockResolvedValue({
      data: { code: 200, message: 'ok', data: { id: 1 }, timestamp: Date.now() },
    });

    // Act
    const result = await request<{ id: number }>({ url: '/test', method: 'get' });

    // Assert
    expect(result).toEqual({ id: 1 });
  });

  it('响应 code 不为 200 时应抛出 ApiError', async () => {
    // Arrange
    mockHttp.request.mockResolvedValue({
      data: { code: 500, message: '服务器内部错误', data: null, timestamp: Date.now() },
    });

    // Act & Assert
    await expect(request({ url: '/test', method: 'get' })).rejects.toThrow(ApiError);
    await expect(request({ url: '/test', method: 'get' })).rejects.toThrow('服务器内部错误');
  });

  it('响应 message 为空时应使用默认错误消息', async () => {
    // Arrange
    mockHttp.request.mockResolvedValue({
      data: { code: 400, message: '', data: null, timestamp: Date.now() },
    });

    // Act & Assert
    await expect(request({ url: '/test', method: 'get' })).rejects.toThrow('请求失败');
  });
});

describe('requestResponse', () => {
  it('响应 code 为 200 时应返回完整的 ApiResponse', async () => {
    // Arrange
    const apiResponse = { code: 200, message: 'ok', data: 'hello', timestamp: Date.now() };
    mockHttp.request.mockResolvedValue({ data: apiResponse });

    // Act
    const result = await requestResponse<string>({ url: '/test', method: 'get' });

    // Assert
    expect(result).toEqual(apiResponse);
  });

  it('响应 code 不为 200 时应抛出 ApiError', async () => {
    // Arrange
    mockHttp.request.mockResolvedValue({
      data: { code: 403, message: '权限不足', data: null, timestamp: Date.now() },
    });

    // Act & Assert
    await expect(requestResponse({ url: '/test', method: 'get' })).rejects.toThrow(ApiError);
  });
});
