import { describe, it, expect, vi, beforeEach } from 'vitest';

// Mock http 模块 — 使用 vi.hoisted 确保在 vi.mock 工厂之前初始化
const { mockHttp } = vi.hoisted(() => ({
  mockHttp: { request: vi.fn() },
}));
vi.mock('./http', () => ({
  http: mockHttp,
  AUTH_TOKEN_CLEAR_EVENT: 'salessystem:auth:clear-tokens',
}));

const { mockGetCurrentAuthRole } = vi.hoisted(() => ({
  mockGetCurrentAuthRole: vi.fn(),
}));
vi.mock('../utils/authSession', () => ({
  getCurrentAuthRole: mockGetCurrentAuthRole,
}));

import { request, requestResponse } from './request';
import { ApiError } from '../types/api';

beforeEach(() => {
  vi.clearAllMocks();
  mockGetCurrentAuthRole.mockReturnValue(null);
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

  it('响应 code 为 401 时应通知认证上下文清理状态但不直接跳转', async () => {
    // Arrange
    mockGetCurrentAuthRole.mockReturnValue('user');
    const dispatchSpy = vi.spyOn(window, 'dispatchEvent');
    const originalLocation = window.location;
    Object.defineProperty(window, 'location', {
      configurable: true,
      value: { href: 'http://localhost/current' },
    });
    mockHttp.request.mockResolvedValue({
      data: { code: 401, message: '未提供Token', data: null, timestamp: Date.now() },
    });

    try {
      // Act & Assert
      await expect(request({ url: '/test', method: 'get' })).rejects.toThrow('未提供Token');
      expect(dispatchSpy).toHaveBeenCalledWith(expect.any(CustomEvent));
      const event = dispatchSpy.mock.calls[0]?.[0] as CustomEvent;
      expect(event.type).toBe('salessystem:auth:clear-tokens');
      expect(event.detail).toEqual({ role: 'user' });
      expect(window.location.href).toBe('http://localhost/current');
    } finally {
      Object.defineProperty(window, 'location', {
        configurable: true,
        value: originalLocation,
      });
    }
  });

  it('公开请求响应 code 为 401 时不应清理当前登录态', async () => {
    // Arrange
    mockGetCurrentAuthRole.mockReturnValue('user');
    const dispatchSpy = vi.spyOn(window, 'dispatchEvent');
    mockHttp.request.mockResolvedValue({
      data: { code: 401, message: '公开商品不可访问', data: null, timestamp: Date.now() },
    });

    // Act & Assert
    await expect(request({ url: '/public/products', method: 'get', authRole: false })).rejects.toThrow(
      '公开商品不可访问',
    );
    expect(dispatchSpy).not.toHaveBeenCalledWith(expect.any(CustomEvent));
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

  it('公开请求响应 code 为 401 时不应通知认证上下文清理状态', async () => {
    // Arrange
    mockGetCurrentAuthRole.mockReturnValue('user');
    const dispatchSpy = vi.spyOn(window, 'dispatchEvent');
    mockHttp.request.mockResolvedValue({
      data: { code: 401, message: '公开接口未授权', data: null, timestamp: Date.now() },
    });

    // Act & Assert
    await expect(requestResponse({ url: '/public', method: 'get', authRole: false })).rejects.toThrow(
      '公开接口未授权',
    );
    expect(dispatchSpy).not.toHaveBeenCalledWith(expect.any(CustomEvent));
  });
});
